package com.talktrip.talktrip.domain.order.controller;

import com.talktrip.talktrip.domain.order.entity.Order;
import com.talktrip.talktrip.domain.order.enums.OrderStatus;
import com.talktrip.talktrip.domain.order.enums.PaymentMethod;
import com.talktrip.talktrip.domain.order.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Controller
@RequestMapping("/api/tosspay")
public class WidgetController {

    @Value("${toss.secretKey}")
    private String widgetSecretKey;

    private final OrderRepository orderRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public WidgetController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping(value = "/confirm")
    public ResponseEntity<JSONObject> confirmPayment(@RequestBody String jsonBody) throws Exception {

        JSONParser parser = new JSONParser();

        String orderId;
        String amount;
        String paymentKey;

        try {
            JSONObject requestData = (JSONObject) parser.parse(jsonBody);
            paymentKey = (String) requestData.get("paymentKey");
            orderId = (String) requestData.get("orderId");
            amount = (String) requestData.get("amount");
        } catch (ParseException e) {
            throw new RuntimeException("JSON 파싱 오류", e);
        }

        JSONObject requestJson = new JSONObject();
        requestJson.put("orderId", orderId);
        requestJson.put("amount", amount);
        requestJson.put("paymentKey", paymentKey);

        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorization = "Basic " + new String(encodedBytes);

        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));

        int code = connection.getResponseCode();
        boolean isSuccess = code == 200;

        InputStream responseStream = isSuccess ? connection.getInputStream() : connection.getErrorStream();
        Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
        JSONObject responseJson = (JSONObject) parser.parse(reader);
        responseStream.close();

        if (isSuccess) {
            logger.info("결제 응답 전체 JSON: {}", responseJson.toJSONString());

            Optional<Order> optionalOrder = orderRepository.findByOrderCode(orderId);
            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();

                String methodStr = (String) responseJson.get("method"); // "간편결제" 등
                String provider = null;

                if ("간편결제".equals(methodStr) && responseJson.containsKey("easyPay")) {
                    JSONObject easyPayJson = (JSONObject) responseJson.get("easyPay");
                    provider = (String) easyPayJson.get("provider"); // "토스페이", "카카오페이" 등
                }

                logger.info("결제 응답에서 받은 결제 수단(method): {}", methodStr);
                logger.info("간편결제 provider: {}", provider);

                PaymentMethod paymentMethod = mapToPaymentMethod(methodStr, provider);

                order.updatePaymentInfo(paymentMethod, OrderStatus.SUCCESS);
                orderRepository.save(order);
            } else {
                logger.warn("주문 ID를 찾을 수 없습니다: {}", orderId);
            }
        }


        return ResponseEntity.status(code).body(responseJson);
    }

    private PaymentMethod mapToPaymentMethod(String methodStr, String provider) {
        if (methodStr == null) {
            return PaymentMethod.UNKNOWN;
        }
        String method = methodStr.toUpperCase();

        switch (method) {
            case "카드":
                return PaymentMethod.CARD;
            case "계좌이체":
                return PaymentMethod.ACCOUNT;
            case "휴대폰결제":
                return PaymentMethod.MOBILE;
            case "간편결제":
                if (provider != null) {
                    switch (provider) {
                        case "토스페이":
                            return PaymentMethod.TOSSPAY;
                        case "카카오페이":
                            return PaymentMethod.KAKAO;
                        case "페이코":
                            return PaymentMethod.PAYCO;
                        case "네이버페이":
                            return PaymentMethod.NAVER;
                        default:
                            return PaymentMethod.UNKNOWN;
                    }
                }
                return PaymentMethod.UNKNOWN;
            default:
                return PaymentMethod.UNKNOWN;
        }
    }


    @RequestMapping(value = "/success", method = RequestMethod.GET)
    public String paymentRequest(HttpServletRequest request, Model model) throws Exception {
        return "/success";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(HttpServletRequest request, Model model) throws Exception {
        return "/checkout";
    }

    @RequestMapping(value = "/fail", method = RequestMethod.GET)
    public String failPayment(HttpServletRequest request, Model model) throws Exception {
        String failCode = request.getParameter("code");
        String failMessage = request.getParameter("message");

        model.addAttribute("code", failCode);
        model.addAttribute("message", failMessage);

        return "/fail";
    }
}
