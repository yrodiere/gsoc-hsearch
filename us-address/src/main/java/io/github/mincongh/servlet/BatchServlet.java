package io.github.mincongh.servlet;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.github.mincongh.session.BatchSession;

@WebServlet("/batch")
public class BatchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private BatchSession batchSession;
    
    protected void service(HttpServletRequest request,
            HttpServletResponse response) {
        
        System.out.println("batchSession#printId() called");
        batchSession.printId();
//      try {
//          batchSession.printAddressesTop1000();
//      } catch (InterruptedException e) {
//          e.printStackTrace();
//      }
    }
}
