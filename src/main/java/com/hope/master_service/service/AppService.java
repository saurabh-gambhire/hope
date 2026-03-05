package com.hope.master_service.service;

import com.hope.master_service.config.IamConfig;
import com.hope.master_service.dto.enums.TimeZone;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.dto.user.User;
import com.hope.master_service.exception.HopeException;
import com.hope.master_service.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.ZoneId;
import java.util.Map;

@Slf4j
public abstract class AppService {

    @Value("${keycloak.realm}")
    private String realm;

    @Autowired
    protected MessageService messageService;

    protected void throwError(ResponseCode code, String... args) throws HopeException {
        throw new HopeException(code, messageService.getMessage(code, args), args);
    }

    protected void throwError(Exception exception) throws HopeException {
        throw new HopeException(exception);
    }

    protected HopeException throwException(ResponseCode code, String... args) {
        return new HopeException(code, messageService.getMessage(code, args));
    }

    protected User getCurrentUser() throws HopeException {
        try {
            Map<String, Object> tokenAttributes = IamConfig.getTokenAttributes();
            return User.builder()
                    .email(tokenAttributes.containsKey("email") ? tokenAttributes.get("email").toString() : null)
                    .emailVerified(Boolean.parseBoolean(tokenAttributes.containsKey("email_verified") ? tokenAttributes.get("email_verified").toString() : "false"))
                    .active(Boolean.parseBoolean(tokenAttributes.containsKey("active") ? tokenAttributes.get("active").toString() : "false"))
                    .role(IamConfig.getRole(tokenAttributes))
                    .tenantKey(IamConfig.getTenantKeyFromTokensIssuerUrl(tokenAttributes))
                    .iamId(tokenAttributes.containsKey("sub") ? tokenAttributes.get("sub").toString() : null)
                    .build();
        } catch (Exception e) {
            throw new HopeException(e);
        }
    }


    public String getTenantNameFromTenantContext() {
        return TenantContext.getCurrentTenant();
    }

    public String getRealmNameFromTenantContext() {
        return getTenantNameFromTenantContext().equals("public") ? realm : TenantContext.getCurrentTenant();
    }

    protected ZoneId getZoneId(TimeZone timezone) {
        ZoneId zoneId;
        switch (timezone) {
            case EST:
                zoneId = ZoneId.of(ZoneId.SHORT_IDS.get(timezone.name()));
                break;
            case EDT:
                zoneId = ZoneId.of("America/New_York");
                break;
            case PST:
                zoneId = ZoneId.of(ZoneId.SHORT_IDS.get(timezone.name()));
                break;
            case PDT:
                zoneId = ZoneId.of("America/Los_Angeles");
                break;
            case CST:
                zoneId = ZoneId.of(ZoneId.SHORT_IDS.get(timezone.name()));
                break;
            case CDT:
                zoneId = ZoneId.of("America/Chicago");
                break;
            case AST:
                zoneId = ZoneId.of(ZoneId.SHORT_IDS.get(timezone.name()));
                break;
            case ADT:
                zoneId = ZoneId.of("America/Halifax");
                break;
            case MST:
                zoneId = ZoneId.of("America/Phoenix");
                break;
            case MDT:
                zoneId = ZoneId.of("America/Denver");
                break;
            case IST:
                zoneId = ZoneId.of("Asia/Kolkata");
                break;
            case HST:
                zoneId = ZoneId.of("Pacific/Honolulu");
                break;
            case AKDT, AKST:
                zoneId = ZoneId.of("America/Anchorage");
                break;
            default:
                return null;
        }
        return zoneId;
    }

    protected Pageable getPageable(int page, int size, String sortBy, String sortDirection) {
        return PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy);
    }
}
