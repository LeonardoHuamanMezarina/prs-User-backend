package vallegrande.edu.pe.auth.domain.port.in;

import reactor.core.publisher.Mono;
import vallegrande.edu.pe.auth.domain.model.User;

import java.util.List;

public interface AuthUseCase {
    Mono<List<String>> getUserModules(User user);
}
