import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { CartProvider } from "@/context/CartContext";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "IPWORLD - Hệ thống Bán lẻ Công nghệ Chính Hãng",
  description: "Hệ thống mua sắm thiết bị công nghệ chính hãng IPWORLD - Điện thoại, Laptop, PC, Màn hình, Máy tính bảng, Phụ kiện",
  icons: {
    icon: [
      { url: "/HINH_TRANG_CHU/ipwworlkd.png", href: "/HINH_TRANG_CHU/ipwworlkd.png" },
      { url: "/icon.png", href: "/icon.png" },
      { url: "/favicon.ico", href: "/favicon.ico" },
    ],
    shortcut: "/HINH_TRANG_CHU/ipwworlkd.png",
    apple: "/HINH_TRANG_CHU/ipwworlkd.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body className={`${inter.className} bg-slate-50 min-h-screen flex flex-col`}>
        <CartProvider>
          {children}
        </CartProvider>
      </body>
    </html>
  );
}