package com.stockpro.alert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Unread alert count for a recipient.")
public class UnreadCountResponse {

    @Schema(example = "101")
    private Long recipientId;

    @Schema(example = "5")
    private Long unreadCount;
}
