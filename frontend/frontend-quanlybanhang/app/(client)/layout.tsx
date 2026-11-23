import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";

export default function ClientLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div
      className="min-h-screen flex flex-col bg-cover bg-center bg-no-repeat bg-fixed selection:bg-blue-600 selection:text-white"
      style={{ backgroundImage: "url('/HINH_TRANG_CHU/galaxy.jpg')" }}
    >
      <div className="sticky top-0 z-50 pt-3 pb-2 px-2 sm:px-4">
        <Navbar />
      </div>

      <main className="flex-grow flex flex-col px-2 sm:px-4 py-4 max-w-[1440px] mx-auto w-full">
        {children}
      </main>

      <Footer />
    </div>
  );
}
