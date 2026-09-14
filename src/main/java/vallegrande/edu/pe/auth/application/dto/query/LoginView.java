package vallegrande.edu.pe.auth.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginView {
    private String token;
    private Long id;
    private String name;
    private String lastname;
    private String email;
    private String role;
    private Long tenantId;
    private String status;
    private String redirectTo;
    private List<String> modules;
}
