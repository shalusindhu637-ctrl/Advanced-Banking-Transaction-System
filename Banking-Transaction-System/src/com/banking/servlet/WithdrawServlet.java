package com.banking.servlet;

import com.banking.dao.CustomerDAO;
import com.banking.dao.CustomerDAO.TxResult;
import com.banking.dao.TransactionSecurityDAO;
import com.banking.dao.TransactionSecurityDAO.PinResult;
import com.banking.model.Customer;
import com.banking.util.CsrfUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/withdraw")
public class WithdrawServlet extends HttpServlet {

    private CustomerDAO customerDAO = new CustomerDAO();
    private TransactionSecurityDAO securityDAO = new TransactionSecurityDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        request.getRequestDispatcher("withdraw.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        if (session == null || session.getAttribute("customerId") == null) {
            if (ajax) { json(response, 401, false, "AUTH", "Authentication required.", 0, 0, null, null, null); return; }
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        if (!CsrfUtil.isValid(request)) {
            if (ajax) { json(response, 403, false, "INVALID_CSRF", "Invalid or expired form submission. Please try again.", 0, 0, null, null, null); return; }
            request.setAttribute("errorMessage", "Invalid or expired form submission. Please try again.");
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
            return;
        }

        int customerId = (Integer) session.getAttribute("customerId");
        String amountStr = request.getParameter("amount");
        String pin = request.getParameter("pin");
        double amount;
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) throw new NumberFormatException();
            amount = Double.parseDouble(amountStr.trim());
        } catch (NumberFormatException e) {
            if (ajax) { json(response, 400, false, "INVALID", "Invalid amount. Please enter a valid number.", 0, 0, null, null, null); return; }
            request.setAttribute("errorMessage", "Invalid amount. Please enter a valid number.");
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
            return;
        }
        if (amount <= 0) {
            if (ajax) { json(response, 400, false, "INVALID", "Amount must be greater than zero.", 0, 0, null, null, null); return; }
            request.setAttribute("errorMessage", "Amount must be greater than zero.");
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
            return;
        }
        if (amount > 50000) {
            if (ajax) { json(response, 400, false, "LIMIT", "Amount exceeds maximum limit of \u20B950,000 per transaction.", 0, 0, null, null, null); return; }
            request.setAttribute("errorMessage", "Amount exceeds maximum limit of \u20B950,000 per transaction.");
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
            return;
        }

        // Server-side PIN verification (existing backend logic; required for withdrawal)
        PinResult pv = securityDAO.verifyPin(customerId, pin);
        if (!pv.ok) {
            int st = "LOCKED".equals(pv.code) || "INVALID".equals(pv.code) || "NOT_SET".equals(pv.code) ? 403 : 400;
            if (ajax) { json(response, st, false, pv.code, pv.message, 0, 0, null, null, null); return; }
            request.setAttribute("errorMessage", pv.message);
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
            return;
        }

        // Server-side balance check + DB transaction
        TxResult r = customerDAO.withdrawResult(customerId, amount);
        if (r.ok) {
            CsrfUtil.rotate(request);
            Customer updated = customerDAO.getCustomerById(customerId);
            session.setAttribute("customer", updated);
            String newToken = CsrfUtil.getToken(request);
            if (ajax) { json(response, 200, true, "OK", "Withdrawal successful.", amount, r.balance, r.transactionId, r.date, newToken); return; }
            request.setAttribute("successMessage", "Amount withdrawn successfully. New balance: \u20B9" + String.format("%,.2f", r.balance));
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
        } else {
            int st = r.message != null && r.message.toLowerCase().contains("insufficient") ? 400 : 400;
            if (ajax) { json(response, st, false, "FAILED", r.message, 0, r.balance, null, null, null); return; }
            request.setAttribute("errorMessage", r.message);
            request.getRequestDispatcher("withdraw.jsp").forward(request, response);
        }
    }

    private void json(HttpServletResponse response, int status, boolean ok, String code, String message, double amount, double balance, String txId, String date, String csrfToken) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"ok\":").append(ok)
          .append(",\"success\":").append(ok)
          .append(",\"code\":\"").append(esc(code)).append("\"")
          .append(",\"message\":\"").append(esc(message)).append("\"");
        if (ok) {
            sb.append(",\"amount\":").append(String.format(java.util.Locale.US, "%.2f", amount));
            sb.append(",\"balance\":").append(String.format(java.util.Locale.US, "%.2f", balance));
            sb.append(",\"transactionId\":\"").append(esc(txId == null ? "" : txId)).append("\"");
            sb.append(",\"date\":\"").append(esc(date == null ? "" : date)).append("\"");
            sb.append(",\"csrfToken\":\"").append(esc(csrfToken == null ? "" : csrfToken)).append("\"");
        } else if (balance > 0) {
            sb.append(",\"balance\":").append(String.format(java.util.Locale.US, "%.2f", balance));
        }
        sb.append("}");
        try (PrintWriter out = response.getWriter()) { out.print(sb.toString()); }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
