package com.erp.scheduler.task;

/**
 * Interface cho các tác vụ lập lịch định kỳ trong hệ thống ERP.
 * Mọi Task Handler mới chỉ cần implement interface này và khai báo @Component.
 */
public interface ScheduledTaskHandler {

    /**
     * Mã định danh duy nhất của loại tác vụ (khớp với jobType trong ScheduledJob)
     */
    String getJobType();

    /**
     * Tên mô tả thân thiện của loại tác vụ
     */
    String getDisplayName();

    /**
     * Thực thi tác vụ với tham số cấu hình tùy biến
     *
     * @param paramsJson  Tham số định dạng JSON
     * @param triggeredBy Tên người kích hoạt hoặc SYSTEM_CRON
     * @return Tóm tắt kết quả thực thi
     * @throws Exception nếu xảy ra lỗi trong quá trình thực hiện
     */
    String execute(String paramsJson, String triggeredBy) throws Exception;
}

