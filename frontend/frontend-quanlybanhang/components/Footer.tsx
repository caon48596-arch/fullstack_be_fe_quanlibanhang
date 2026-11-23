"use client";

import React from 'react';
import { footerStyles } from '@/styles/constants';

export default function Footer() {
    return (
        <footer className={footerStyles.wrapper}>
            <div className={footerStyles.grid}>
                <div>
                    <h3 className={footerStyles.columnTitle}>
                        IPWORLD
                    </h3>
                    <p className={footerStyles.description}>
                        Hệ thống bán lẻ điện thoại, máy tính, phụ kiện chính hãng uy tín hàng đầu Việt Nam.
                    </p>
                    <p className={footerStyles.commitment}>
                        Cam kết chất lượng - Giá rẻ mỗi ngày.
                    </p>
                </div>

                <div>
                    <h3 className={footerStyles.columnTitle}>
                        Chính sách hỗ trợ
                    </h3>
                    <ul className={footerStyles.list}>
                        <li className={footerStyles.listItem}>
                            <img src="/HINH_TRANG_CHU/chínhachbaohang.png" alt="Bảo hành" className={footerStyles.icon} />
                            <span>Chính sách bảo hành chính hãng</span>
                        </li>
                        <li className={footerStyles.listItem}>
                            <img src="/HINH_TRANG_CHU/tra30day.png" alt="Đổi trả" className={footerStyles.icon} />
                            <span>Chính sách đổi trả trong 30 ngày</span>
                        </li>
                        <li className={footerStyles.listItem}>
                            <img src="/HINH_TRANG_CHU/tragop.png" alt="Trả góp" className={footerStyles.icon} />
                            <span>Hướng dẫn mua trả góp 0% lãi suất</span>
                        </li>
                        <li className={footerStyles.listItem}>
                            <img src="/HINH_TRANG_CHU/giaohang.png" alt="Giao hàng" className={footerStyles.icon} />
                            <span>Chính sách giao hàng tận nơi siêu tốc</span>
                        </li>
                    </ul>
                </div>

                <div>
                    <h3 className={footerStyles.columnTitle}>
                        Thông tin liên hệ
                    </h3>
                    <ul className={footerStyles.list}>
                        <li className="flex items-center gap-2">
                            <img src="/HINH_TRANG_CHU/DINHVI.png" alt="Địa chỉ" className={footerStyles.icon} />
                            <span>Số 413 đường 30/4, P. Tân An, TP. Cần Thơ</span>
                        </li>
                        <li className="flex items-center gap-2">
                            <img src="/HINH_TRANG_CHU/lienhe.png" alt="Hotline" className={footerStyles.icon} />
                            <span>Hotline: <strong className="text-white">1800.1234</strong> (Miễn phí)</span>
                        </li>
                        <li className="flex items-center gap-2">
                            <img src="/HINH_TRANG_CHU/EMAIL.png" alt="Email" className={footerStyles.icon} />
                            <span>Email: cskh@IPWORLD.vn</span>
                        </li>
                        <li className="flex items-center gap-2">
                            <img src="/HINH_TRANG_CHU/ONBORAD.png" alt="Giờ mở cửa" className={footerStyles.icon} />
                            <span>Giờ mở cửa: 8:00 - 22:00 (Tất cả các ngày)</span>
                        </li>
                    </ul>
                </div>
            </div>

            <div className={footerStyles.bottomBar}>
                <p>© 2026 Bản quyền thuộc về IPWORLD. Thiết kế và phát triển bởi TEAM-NHD.</p>
            </div>
        </footer>
    );
}
