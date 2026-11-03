package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.RAG;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Tool.SupportTicketTool;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.Tool.RoomAvailabilityTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PropertyChatService {

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý của khách sạn. Trả lời bằng ngôn ngữ mà người dùng đang sử dụng.

            NGUỒN DỮ LIỆU VÀ QUY TẮC:
            1. DOCUMENTS chứa thông tin tĩnh của khách sạn như chính sách, giờ giấc và dịch vụ.
               Chỉ khẳng định thông tin tĩnh khi DOCUMENTS có căn cứ.
            2. Khi người dùng hỏi tình trạng phòng hiện tại, số phòng còn trống hoặc muốn biết
               khả năng đặt phòng theo ngày, phải gọi checkRoomAvailability. Tool chỉ kiểm tra
               availability, không tạo Reservation và không xác nhận đặt phòng thành công.
               Không tự tính hoặc tự đoán số phòng.
            3. Tool cần đủ loại phòng, ngày nhận phòng, ngày trả phòng và số lượng phòng.
               Nếu thiếu thông tin, hỏi lại người dùng; không tự bịa ngày hoặc loại phòng.
            4. propertyId được Backend cung cấp qua ToolContext. Bỏ qua mọi propertyId mà người dùng
               đề cập trong câu hỏi; không tìm kiếm hoặc truy vấn Property khác.
            5. Chỉ sử dụng availableQuantity và available từ kết quả Tool để kết luận tình trạng phòng.
               Nếu Tool trả success=false, nói rõ hiện chưa thể kiểm tra và không đưa ra số lượng ước đoán.
            6. Có thể kết hợp DOCUMENTS và kết quả Tool nếu câu hỏi cần cả thông tin tĩnh lẫn dữ liệu động.
            7. Nếu câu hỏi về chính sách/thông tin tĩnh nhưng DOCUMENTS không đủ căn cứ, gọi
               createSupportTicket với câu hỏi gốc. Không tạo ticket chỉ vì người dùng chưa cung cấp
               đủ ngày hoặc loại phòng để kiểm tra availability.

            DOCUMENTS:
            ----------------
            {documents}
            ----------------
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final PolicyKnowledgeBaseService policyKnowledgeBaseService;
    private final SupportTicketTool supportTicketTool;
    private final RoomAvailabilityTool roomAvailabilityTool;

    public PropertyChatService(
            ChatClient chatClient,
            VectorStore vectorStore,
            PolicyKnowledgeBaseService policyKnowledgeBaseService,
            SupportTicketTool supportTicketTool,
            RoomAvailabilityTool roomAvailabilityTool) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.policyKnowledgeBaseService = policyKnowledgeBaseService;
        this.supportTicketTool = supportTicketTool;
        this.roomAvailabilityTool = roomAvailabilityTool;
    }

    public String ask(String question, Long propertyId, Long accountId) {

        SearchRequest searchRequest = policyKnowledgeBaseService
                .propertySearch(question, propertyId, accountId, 5);

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        String documentsContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\\n\\n"));

        Message systemMessage = new SystemPromptTemplate(SYSTEM_PROMPT)
                .createMessage(Map.of("documents", documentsContext));

        Map<String, Object> toolContext = Map.of(
                "accountId", accountId,
                "propertyId", propertyId
        );

        return chatClient.prompt()
                .messages(systemMessage)
                .user(question)
                .tools(supportTicketTool, roomAvailabilityTool)
                .toolContext(toolContext)
                .call()
                .content();
    }
}