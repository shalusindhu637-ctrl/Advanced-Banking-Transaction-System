package com.banking.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter({"/dashboard.jsp", "/account.jsp", "/deposit.jsp", "/withdraw.jsp", "/balance.jsp", "/transactions.jsp", "/profile.jsp",
        "/set-pin.jsp", "/dashboard", "/deposit", "/withdraw", "/balance", "/transactions", "/profile", "/set-pin"})
public class AuthFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute("customerId") == null) {
            boolean ajax = "XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With"))
                    || (httpRequest.getHeader("Accept") != null && httpRequest.getHeader("Accept").contains("application/json"));
            if (ajax) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.setHeader("Cache-Control", "no-store");
                httpResponse.getWriter().print("{\"success\":false,\"ok\":false,\"code\":\"AUTH\",\"message\":\"Authentication required.\"}");
                return;
            }
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp");
            return;
        }

        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
    }
}
