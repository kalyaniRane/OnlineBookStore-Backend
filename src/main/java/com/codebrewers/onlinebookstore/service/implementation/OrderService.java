package com.codebrewers.onlinebookstore.service.implementation;

import com.codebrewers.onlinebookstore.exception.CartException;
import com.codebrewers.onlinebookstore.exception.UserServiceException;
import com.codebrewers.onlinebookstore.model.*;
import com.codebrewers.onlinebookstore.repository.*;
import com.codebrewers.onlinebookstore.service.IOrderService;
import com.codebrewers.onlinebookstore.utils.implementation.MailService;
import com.codebrewers.onlinebookstore.utils.implementation.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.mail.MessagingException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService implements IOrderService {

    @Autowired
    Token jwtToken;
    @Autowired
    MailService mailService;
    @Autowired
    IOrderRepository orderRepository;
    @Autowired
    IBookCartDetailsRepository cartDetailsRepository;
    @Autowired
    IBookStoreRepository bookStoreRepository;
    @Autowired
    IUserRepository userRepository;
    @Autowired
    ICustomerDetailsRepository customerDetailsRepository;
    @Autowired
    ICartRepository cartRepository;


    @Override
    public Integer placeOrder(Double totalPrice, String token) throws MessagingException {
        Integer orderId = generatedOrderId();
        System.out.println(orderId);
        CartDetails cart = getCart(token);
        List<BookCartDetails> cartBooks = cartDetailsRepository.fetchCartItems(cart.getId());
        CustomerDetails customerDetails = customerDetailsRepository.findByUserDetailsOrderById(cart.getUserDetails()).get(0);
        OrderDetails order = new OrderDetails(cart, orderId, cart.getUserDetails(), totalPrice, customerDetails, cartBooks);
        orderRepository.save(order);
        cartBooks.forEach(cartBook ->{
            cartBook.setOrderDetails(order);
            bookStoreRepository.updateBookQuantity(cartBook.getBookDetails().id, cartBook.getQuantity());
        });
        cartDetailsRepository.updateOrderPlacedStatus(cart.getId());

        String body = "Dear, "+cart.getUserDetails().fullName+" Congratulations! Your order for the books is Successfully Placed. Save this orderId: #"+orderId+" for further communication"
                +"\n Your Book Name Are : "+
                cart.getBook().stream().map(bookCartDetails -> bookCartDetails.getBookDetails().bookName).peek(System.out::println).collect(Collectors.toList()) +"\n Total Book Price : "+totalPrice+"\n Total No. Of Books : "+cart.getBook().size();

        mailService.sendMail(body,"Order Placed",cart.userDetails.emailID);
        return orderId;
    }

    private int generatedOrderId(){
        boolean isUnique = false;
        Integer orderId = 0;
        while(!isUnique){
            orderId = (int) Math.floor(100000 + Math.random() * 999999);
            Optional<OrderDetails> byId = orderRepository.findByOrderId(orderId);
            if( !byId.isPresent())
                isUnique = true;
        }
        System.out.println(orderId);
        return orderId;
    }

    private CartDetails getCart(String token) {
        int userId = jwtToken.decodeJWT(token);
        UserDetails userDetails = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException("User Not Exist"));
        return cartRepository.findByUserDetails(userDetails)
                .orElseThrow(() -> new CartException("Cart Not Found"));
    }

}