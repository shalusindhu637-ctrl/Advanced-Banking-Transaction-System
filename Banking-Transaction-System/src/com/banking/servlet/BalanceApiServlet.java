package com.banking.servlet;

import com.banking.dao.TransactionSecurityDAO;
import com.banking.dao.TransactionSecurityDAO.BalanceResult;
import com.banking.util.CsrfUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Returns balance ONLY after server-side session auth + CSRF + PIN verification.
 * Never accepts customer/account ID from client. Read-only (no balance/transaction changes).
 */
@WebServlet("/balance-api")
public class BalanceApiServlet extends HttpServlet {
    private final TransactionSecurityDAO dao = new TransactionSecurityDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            json(response, false, "AUTH", "Authentication required", null); return;
        }
        if (!CsrfUtil.isValid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            json(response, false, "CSRF", "Invalid or missing request token.", null); return;
        }
        int customerId = (Integer) session.getAttribute("customerId"); // session-only (prevents IDOR)
        String pin = request.getParameter("pin");
        BalanceResult r = dao.verifyPinAndGetBalance(customerId, pin);
        if (r.ok && r.balance != null) {
            BigDecimal b = r.balance.setScale(2, RoundingMode.HALF_UP);
            json(response, true, "OK", null, "₹" + b.toPlainString());
        } else {
            // generic/controlled messages; do not reveal correctness
            int status = "LOCKED".equals(r.code) ? 429 : 400;
            response.setStatus(status);
            json(response, false, r.code == null ? "ERR" : r.code, r.message == null ? "Unable to verify PIN." : r.message, null);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setContentType("application/json;charset=UTF-8");
        json(response, false, "METHOD", "Not allowed.", null);
    }

    private static void json(HttpServletResponse response, boolean ok, String code, String message, String balance) throws IOException {
        try (PrintWriter w = response.getWriter()) {
            w.write("{\"success\":" + ok + ",\"ok\":" + ok + ",\"code\":\"" + esc(code) + "\"");
            if (message != null) w.write(",\"message\":\"" + esc(message) + "\"");
            if (balance != null) w.write(",\"balance\":\"" + esc(balance) + "\"");
            w.write("}");
        }
    }
    private static String esc(String s) { if (s==null) return ""; return s.replace("\\","\\\\").replace("\"","\\\""); }
}