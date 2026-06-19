package dk.utd.fordel.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.PasswordExpiryService;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;

@Component
public class PasswordExpiryInterceptor implements HandlerInterceptor {

    private static final String LOGIN_PATH = "/login";
    private static final String PASSWORD_EXPIRED_PATH = "/password-expired";
    private static final String UPDATE_PASSWORD_PATH = "/update-password";
    private static final String CHANGE_PASSWORD_PATH = "/change-password";
    private static final String LOGOUT_PATH = "/logout";

    private final PasswordExpiryService passwordExpiryService;
    private final UnitOfWork unitOfWork;

    public PasswordExpiryInterceptor(PasswordExpiryService passwordExpiryService, UnitOfWork unitOfWork) {
        this.passwordExpiryService = passwordExpiryService;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }

        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return true;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isAllowedWhenExpired(path)) {
            return true;
        }

        try (var uow = unitOfWork.begin()) {
            boolean expired = passwordExpiryService.isPasswordExpired(auth.user());
            uow.commit();

            if (expired) {
                response.sendRedirect(request.getContextPath() + PASSWORD_EXPIRED_PATH);
                return false;
            }
        } catch (UserDoesNotExist e) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + LOGIN_PATH);
            return false;
        } catch (UnitOfWorkException e) {
            throw new RuntimeException("Could not check password expiry", e);
        }

        return true;
    }

    private boolean isAllowedWhenExpired(String path) {
        return PASSWORD_EXPIRED_PATH.equals(path)
                || LOGIN_PATH.equals(path)
                || UPDATE_PASSWORD_PATH.equals(path)
                || CHANGE_PASSWORD_PATH.equals(path)
                || LOGOUT_PATH.equals(path)
                || path.startsWith("/js/")
                || path.startsWith("/css/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || "/style.css".equals(path)
                || "/favicon.ico".equals(path);
    }
}