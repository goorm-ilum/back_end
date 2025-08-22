# JPA 엔티티 관계 방식 선택 가이드: 연관관계 vs ID 기반

## 📋 목차
1. 개요
2. 연관관계 방식
3. ID 기반 방식
4. 선택 기준
5. 실제 예시
6. 성능 비교
7. 체크리스트

## 개요

JPA에서 엔티티 간의 관계를 표현할 때 두 가지 방식을 선택할 수 있습니다:

- **연관관계 방식**: @ManyToOne, @OneToMany 등으로 엔티티 객체를 직접 참조
- **ID 기반 방식**: 단순히 외래키 ID 값만 저장

각 방식의 특징과 언제 사용해야 하는지 알아보겠습니다.

## 연관관계 방식

### 특징
- JPA의 @ManyToOne, @OneToMany, @OneToOne 어노테이션 사용
- 엔티티 객체를 직접 참조
- 외래키 제약조건으로 데이터 무결성 보장
- 객체지향적 설계

### 장점
[O] **타입 안전성**: 컴파일 타임에 타입 체크  
[O] **편의성**: 엔티티 객체 직접 접근 가능  
[O] **데이터 무결성**: 외래키 제약조건으로 보장  
[O] **객체지향적**: 도메인 객체 간의 관계 명확  

### 단점
[X] **성능 오버헤드**: JOIN 쿼리 발생  
[X] **메모리 사용량**: 관련 엔티티 객체들도 함께 로드  
[X] **강한 결합**: 도메인 간 의존성 증가  
[X] **N+1 문제**: 연관 엔티티 조회 시 추가 쿼리 발생  

### 사용 예시

<pre><code>
@Entity
public class Order {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
    
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    
    // 복잡한 비즈니스 로직
    public int calculateTotalAmount() {
        return orderItems.stream()
                .mapToInt(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
}
</code></pre>

## ID 기반 방식

### 특징
- 단순히 외래키 ID 값만 저장
- @Column 어노테이션으로 ID 필드 정의
- 엔티티 간 직접적인 참조 없음
- 도메인 독립성 확보

### 장점
[O] **성능**: 단순 조회로 빠른 속도  
[O] **메모리 효율성**: 필요한 데이터만 로드  
[O] **확장성**: 도메인 간 약한 결합  
[O] **유연성**: 필요할 때만 관련 데이터 조회  

### 단점
[X] **타입 안전성**: ID 값만으로 타입 체크 불가  
[X] **편의성**: 관련 데이터 조회 시 별도 쿼리 필요  
[X] **복잡성**: 수동으로 관계 관리 필요  
[X] **데이터 무결성**: 애플리케이션 레벨에서 관리  

### 사용 예시

<pre><code>
@Entity
public class Like {
    @Id
    private Long id;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "member_id")
    private Long memberId;
    
    // 단순한 참조만 필요
    public boolean isLikedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
</code></pre>

## 선택 기준

### 연관관계 방식이 적합한 경우

#### 1. 복잡한 비즈니스 로직이 있는 경우

<pre><code>
// 주문 시스템 - 주문과 주문상품이 항상 함께 처리됨
@Entity
public class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
    
    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
        orderItems.forEach(OrderItem::cancelItem); // 연관 엔티티와 함께 처리
    }
}
</code></pre>

#### 2. 자주 함께 조회되는 경우

<pre><code>
// 게시판 시스템 - 게시글과 댓글이 항상 함께 표시됨
@Entity
public class Post {
    @OneToMany(mappedBy = "post")
    private List<Comment> comments;
    
    public PostDetailResponse toDetailResponse() {
        return PostDetailResponse.builder()
                .title(this.title)
                .content(this.content)
                .comments(this.comments.stream()
                        .map(Comment::toResponse)
                        .toList())
                .build();
    }
}
</code></pre>

#### 3. 데이터 무결성이 중요한 경우

<pre><code>
// 회원-프로필 시스템 - 함께 생성/삭제되어야 함
@Entity
public class Member {
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL)
    private Profile profile;
    
    // 회원 삭제 시 프로필도 함께 삭제
}
</code></pre>

#### 4. 도메인 간 강한 결합이 필요한 경우

<pre><code>
// 상품-상품옵션 시스템 - 상품 없이는 옵션이 의미 없음
@Entity
public class Product {
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductOption> options;
    
    public ProductOption findOptionByName(String name) {
        return options.stream()
                .filter(option -> option.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
</code></pre>

### ID 기반 방식이 적합한 경우

#### 1. 단순한 참조만 필요한 경우

<pre><code>
// 좋아요 시스템 - 단순히 "누가 무엇을 좋아요했는지"만 기록
@Entity
public class Like {
    private Long productId;
    private Long memberId;
    
    // 상품이나 회원의 상세 정보는 거의 필요 없음
}
</code></pre>

#### 2. 성능이 중요한 경우

<pre><code>
// 로그 시스템 - 대량 데이터 처리, 성능이 중요
@Entity
public class AccessLog {
    private Long userId;
    private String action;
    private LocalDateTime timestamp;
    
    // 사용자 정보는 필요할 때만 별도 조회
}
</code></pre>

#### 3. 도메인이 독립적인 경우

<pre><code>
// 알림 시스템 - 독립적인 생명주기
@Entity
public class Notification {
    private Long userId;
    private Long productId;
    private String message;
    
    // 상품이나 사용자가 삭제되어도 알림은 유지
}
</code></pre>

#### 4. 확장성이 중요한 경우

<pre><code>
// 통계 시스템 - 다양한 도메인과 연결
@Entity
public class Statistics {
    private Long userId;
    private Long productId;
    private Long orderId;
    private String eventType;
    
    // 여러 도메인과 연결되지만 독립적으로 관리
}
</code></pre>

## 실제 예시

### 연관관계 방식 예시

#### 주문 시스템

<pre><code>
@Entity
public class Order {
    @Id
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
    
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;
    
    // 복잡한 비즈니스 로직
    public int calculateTotalAmount() {
        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }
    
    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
        orderItems.forEach(OrderItem::cancelItem);
        payment.refund();
    }
}
</code></pre>

#### 게시판 시스템

<pre><code>
@Entity
public class Post {
    @Id
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    private Member author;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostImage> images;
    
    // 게시글 조회 시 항상 함께 표시되는 정보들
    public PostDetailResponse toDetailResponse() {
        return PostDetailResponse.builder()
                .title(this.title)
                .content(this.content)
                .authorName(this.author.getName())
                .commentCount(this.comments.size())
                .comments(this.comments.stream()
                        .map(Comment::toResponse)
                        .toList())
                .images(this.images.stream()
                        .map(PostImage::getUrl)
                        .toList())
                .build();
    }
}
</code></pre>

### ID 기반 방식 예시

#### 좋아요 시스템

<pre><code>
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "member_id"})
})
public class Like {
    @Id
    private Long id;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    
    // 단순한 참조만 필요
    public boolean isLikedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
</code></pre>

#### 로그 시스템

<pre><code>
@Entity
public class UserActivityLog {
    @Id
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "product_id")
    private Long productId;
    
    private String action;
    private LocalDateTime timestamp;
    private String ipAddress;
    
    // 로그는 대량 생성되므로 성능이 중요
    // 사용자나 상품 정보는 필요할 때만 별도 조회
}
</code></pre>

#### 알림 시스템

<pre><code>
@Entity
public class Notification {
    @Id
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "order_id")
    private Long orderId;
    
    private String message;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    
    // 알림은 독립적인 생명주기
    // 관련 엔티티가 삭제되어도 알림은 유지
}
</code></pre>

## 성능 비교

### 쿼리 수 비교

#### 연관관계 방식 (N+1 문제)

<pre><code>
// 좋아요 100개 조회 시
List<Like> likes = likeRepository.findByMemberId(memberId);
// 쿼리 1: Like + Product + Member 조회 (JOIN)
// 쿼리 100: 각 Product의 Review 조회 (N+1 문제)
// 총 101번의 쿼리 발생!
</code></pre>

#### ID 기반 방식 (최적화)

<pre><code>
// 좋아요 100개 조회 시
List<Like> likes = likeRepository.findByMemberId(memberId);
// 쿼리 1: Like만 조회
// 쿼리 1: Product + Review + 평점 일괄 조회
// 총 2번의 쿼리만 발생!
</code></pre>

### 메모리 사용량 비교

#### 연관관계 방식

<pre><code>
// Like 1000개 조회 시
List<Like> likes = likeRepository.findByMemberId(memberId);
// 메모리에 로드되는 것:
// - Like 객체 1000개
// - Product 객체 1000개 (중복 가능)
// - Member 객체 1개
// 총: 2001개 객체
</code></pre>

#### ID 기반 방식

<pre><code>
// Like 1000개 조회 시
List<Like> likes = likeRepository.findByMemberId(memberId);
// 메모리에 로드되는 것:
// - Like 객체 1000개만
// 총: 1000개 객체
</code></pre>

## 체크리스트

### 연관관계 방식 선택 시 체크리스트
□ 두 엔티티가 항상 함께 처리되나?
□ 복잡한 비즈니스 로직이 있나?
□ 데이터 무결성이 중요하나?
□ 성능보다 편의성이 중요한가?
□ 자주 함께 조회되나?
□ 도메인 간 강한 결합이 필요한가?

### ID 기반 방식 선택 시 체크리스트
□ 단순한 참조만 필요한가?
□ 성능이 중요한가?
□ 도메인이 독립적인가?
□ 대량 데이터를 처리하나?
□ 확장성이 중요한가?
□ 메모리 사용량을 최소화해야 하나?

## 결론

### 연관관계 방식 선택 시기
- **복잡한 비즈니스 로직**이 있는 경우
- **자주 함께 조회**되는 경우
- **데이터 무결성**이 중요한 경우
- **도메인 간 강한 결합**이 필요한 경우

### ID 기반 방식 선택 시기
- **단순한 참조**만 필요한 경우
- **성능**이 중요한 경우
- **도메인 독립성**이 필요한 경우
- **대량 데이터 처리**가 필요한 경우

### 핵심 원칙
1. **비즈니스 요구사항**을 우선 고려
2. **성능과 확장성**을 함께 고려
3. **도메인 설계**에 맞는 방식 선택
4. **유지보수성**을 고려한 선택

---

**참고**: 이 가이드는 일반적인 상황을 기준으로 작성되었습니다. 실제 프로젝트에서는 구체적인 요구사항과 제약사항을 고려하여 적절한 방식을 선택하시기 바랍니다.
