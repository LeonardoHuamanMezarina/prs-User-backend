package vallegrande.edu.pe.auth.domain.exception;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long id) {
        super("Usuario con ID " + id + " no encontrado");
    }
}
