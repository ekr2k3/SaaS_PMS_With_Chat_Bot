
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.enums;

/**
 * Danh sách tên Room Type do SaaS quy định cho MVP.
 * Lưu xuống database bằng tên enum (EnumType.STRING),
 * không lưu ordinal.
 */
public enum RoomTypeName {
    STANDARD,
    SUPERIOR,
    DELUXE,
    EXECUTIVE,
    SUITE,
    FAMILY,
    PRESIDENTIAL
}