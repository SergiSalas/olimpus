package com.sergisalas.olimpus.auth.adapter.in;

import com.sergisalas.olimpus.auth.application.AuthenticateSession;
import com.sergisalas.olimpus.auth.domain.Account;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Turns the {@code Authorization: Bearer <token>} header into an account.
 * It is the only place that knows how the session travels over HTTP.
 */
@Component
public class CurrentAccountResolver implements HandlerMethodArgumentResolver {

    private static final String PREFIX = "Bearer ";

    private final AuthenticateSession authenticateSession;

    public CurrentAccountResolver(AuthenticateSession authenticateSession) {
        this.authenticateSession = authenticateSession;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAccount.class)
                && Account.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mav,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = request == null ? null : request.getHeader("Authorization");
        String token = header != null && header.startsWith(PREFIX) ? header.substring(PREFIX.length()) : null;

        return authenticateSession.execute(token).orElseThrow(NotAuthenticatedException::new);
    }
}
