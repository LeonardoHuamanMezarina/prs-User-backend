package vallegrande.edu.pe.auth.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserCommand {
    private String name;
    private String lastname;
    private String phone;
    private String dni;
    private String role;
    private Long tenantId;
}
