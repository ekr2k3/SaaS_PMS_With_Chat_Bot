package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Tool;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SupportTicket;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportTicketTool {

    private final SupportTicketService supportTicketService;

    @Tool(description = """
        Tạo yêu cầu hỗ trợ khi không thể trả lời câu hỏi của người dùng
        dựa trên dữ liệu RAG. Nội dung câu hỏi phải được giữ nguyên.
        """)
    public String createSupportTicket(
            @ToolParam(description = "Câu hỏi cần hỗ trợ")
            String question,
            ToolContext toolContext) {

        Long accountId = (Long) toolContext.getContext()
                .get("accountId");

        SupportTicket ticket =
                supportTicketService.createTicket(accountId, question);

        return "Đã tạo yêu cầu hỗ trợ với mã ticket: "
                + ticket.getSupportTicketId()
                + ", trạng thái: "
                + ticket.getStatus();
    }
}
