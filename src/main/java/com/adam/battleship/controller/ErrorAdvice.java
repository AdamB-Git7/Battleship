package com.adam.battleship.controller;

import com.adam.battleship.dto.ErrorMessage;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class ErrorAdvice {
    @MessageExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @SendToUser("/queue/errors")
    public ErrorMessage handleError(RuntimeException e) {
        return new ErrorMessage(e.getMessage());
    }
}
