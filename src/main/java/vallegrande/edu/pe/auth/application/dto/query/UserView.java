package vallegrande.edu.pe.auth.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserView {
    private Long id;
    private Long tenantId;
    private String name;
    private String lastname;
    private String email;
    private String phone;
    private String dni;
    private String status;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
