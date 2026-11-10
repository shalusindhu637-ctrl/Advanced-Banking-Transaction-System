package com.banking.servlet;

import com.banking.dao.TransactionSecurityDAO;
import com.banking.dao.TransactionSecurityDAO.PinResult;
import com.banking.dao.TransactionSecurityDAO.PinStatus;
import com.banking.util.CsrfUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/set-pin")
public class SetPinServlet extends HttpServlet {
    private final TransactionSecurityDAO dao = new TransactionSecurityDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) { response.sendRedirect(request.getContextPath()+"/login.jsp"); return; }
        int customerId = (Integer) session.getAttribute("customerId");
        PinStatus st = dao.getPinStatus(customerId);
        request.setAttribute("hasPin", st.hasPin);
        request.getRequestDispatcher("set-pin.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) { response.sendRedirect(request.getContextPath()+"/login.jsp"); return; }
        if (!CsrfUtil.isValid(request)) {
            request.setAttribute("errorMessage", "Invalid request. Please try again.");
            doGet(request, response); return;
        }
        int customerId = (Integer) session.getAttribute("customerId");
        String action = request.getParameter("action");
        PinResult result;
        if ("change".equals(action)) {
            result = dao.changePin(customerId, request.getParameter("currentPin"), request.getParameter("newPin"), request.getParameter("confirmPin"));
        } else {
            result = dao.setPin(customerId, request.getParameter("newPin"), request.getParameter("confirmPin"));
        }
        CsrfUtil.rotate(request);
        if (result.ok) request.setAttribute("successMessage", result.message);
        else request.setAttribute("errorMessage", result.message);
        doGet(request, response);
    }
}