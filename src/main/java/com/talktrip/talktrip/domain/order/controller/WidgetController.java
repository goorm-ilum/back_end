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

        // ✅ 결제 성공 시 Order 업데이트
        if (isSuccess) {
            Optional<Order> optionalOrder = orderRepository.findByOrderCode(orderId);
            if (optionalOrder.isPresent()) {
                Order order = optionalOrder.get();

                String methodStr = (String) responseJson.get("method");  // 예: "CARD", "ACCOUNT_TRANSFER", "MOBILE_PHONE"
                PaymentMethod paymentMethod = mapToPaymentMethod(methodStr);

                order.updatePaymentInfo(paymentMethod, OrderStatus.SUCCESS);
                orderRepository.save(order);
            } else {
                logger.warn("주문 ID를 찾을 수 없습니다: {}", orderId);
            }
        }

        return ResponseEntity.status(code).body(responseJson);
    }

    private PaymentMethod mapToPaymentMethod(String methodStr) {
        switch (methodStr) {
            case "CARD":
                return PaymentMethod.CARD;
            case "ACCOUNT_TRANSFER":
                return PaymentMethod.ACCOUNT;
            case "TOSSPAY":
                return PaymentMethod.TOSSPAY;
            case "PAYCO":
                return PaymentMethod.PAYCO;
            case "KAKAO_PAY":
                return PaymentMethod.KAKAO;
            case "NAVER_PAY":
                return PaymentMethod.NAVER;
            case "MOBILE_PHONE":
                return PaymentMethod.MOBILE;
            default:
                return PaymentMethod.CARD; // 기본값 또는 예외 처리 가능
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
