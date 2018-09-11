package com.leyou.api;


import com.leyou.pojo.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface UserApi {

    @PostMapping("query")
    User queryUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    );
}
