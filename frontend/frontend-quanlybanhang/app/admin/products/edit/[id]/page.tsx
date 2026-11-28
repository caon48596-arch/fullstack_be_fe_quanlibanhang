"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { fetchClient, getValidMediaUrl } from "../../../../../util/fetchClient";
import { productService } from "../../../../../util/productService";
import MediaUploadField from "../../../../../components/MediaUploadField";
import { ArrowLeft, TrendingUp, Image as ImageIcon, Video, Check, Sparkles } from "lucide-react";

export default function EditProduct() {
  const router = useRouter();
  const params = useParams();
  const id = params?.id as string;

  const [formData, setFormData] = useState({
    name: "",
    price: "",
    stock: "",
    description: "",
    imageUrl: "",
    videoUrl: "",
    categoryId: ""
  });
  const [categories, setCategories] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await fetchClient("/categories");
        setCategories(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Lỗi tải danh mục:", err);
      }
    };
    fetchCategories();
  }, []);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        let product: any = null;
        try {
          product = await fetchClient(`/products/${id}`);
        } catch {
          // Fallback to productService
        }
        if (!product) {
          const all = await productService.getAllProducts();
          product = all.find(p => p.id === Number(id));
        }
        if (product) {
          setFormData({
            name: product.name || "",
            price: product.price !== undefined ? product.price.toString() : "0",
            stock: product.stock !== undefined ? product.stock.toString() : "0",
            description: product.description || "",
            imageUrl: product.imageUrl || product.image_url || "",
            videoUrl: product.videoUrl || product.video_url || "",
            categoryId: product.category?.id?.toString() || ""
          });
        }
      } catch (err: any) {
        setError("Không thể tải thông tin sản phẩm.");
      } finally {
        setFetching(false);
      }
    };
    if (id) fetchProduct();
  }, [id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const categoryObj = categories.find(c => c.id === parseInt(formData.categoryId));
      const payload = {
        name: formData.name,
        price: parseFloat(formData.price) || 0,
        stock: parseInt(formData.stock) || 0,
        description: formData.description,
        imageUrl: formData.imageUrl,
        videoUrl: formData.videoUrl,
        category: formData.categoryId ? { id: parseInt(formData.categoryId), name: categoryObj?.name || 'Khác' } : null
      };

      await productService.updateProduct(Number(id), payload);
      
      alert("Cập nhật sản phẩm thành công! Thông tin mới đã được đồng bộ lên Trang chủ.");
      router.push("/");
    } catch (err: any) {
      setError(err.message || "Đã xảy ra lỗi khi cập nhật sản phẩm");
    } finally {
      setLoading(false);
    }
  };

  if (fetching) {
    return (
      <div className="p-8 text-center text-gray-500 text-sm">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-600 mx-auto mb-2"></div>
        Đang tải dữ liệu sản phẩm #{id}...
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-3xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <div className="flex items-center gap-3">
          <Link
            href="/admin/products"
            className="p-2 rounded-lg bg-white border border-gray-200 hover:bg-gray-100 text-gray-700 transition-colors"
            title="Quay lại danh sách"
          >
            <ArrowLeft size={18} />
          </Link>
          <div>
            <h2 className="text-xl sm:text-2xl font-bold text-gray-800">Sửa Sản Phẩm #{id}</h2>
            <p className="text-xs text-gray-500">Chỉnh sửa thông tin hình ảnh, video, tên sản phẩm và giá cả</p>
          </div>
        </div>

        <Link
          href="/admin#daily-revenue"
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition-all"
        >
          <TrendingUp size={16} /> Coi doanh thu theo ngày
        </Link>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
        {error && <p className="text-xs font-bold text-red-500 mb-4 p-3 bg-red-50 rounded-lg border border-red-200">{error}</p>}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Tên sản phẩm <span className="text-red-500">*</span>
              </label>
              <input
                required
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                placeholder="Nhập tên sản phẩm..."
                className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Danh mục <span className="text-red-500">*</span>
              </label>
              <select
                required
                name="categoryId"
                value={formData.categoryId}
                onChange={handleChange}
                className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500 bg-white"
              >
                <option value="">-- Chọn danh mục --</option>
                {categories.map((cat: any) => (
                  <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Giá (₫) <span className="text-red-500">*</span>
              </label>
              <input
                required
                type="number"
                name="price"
                min="0"
                value={formData.price}
                onChange={handleChange}
                className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Số lượng tồn kho <span className="text-red-500">*</span>
              </label>
              <input
                required
                type="number"
                name="stock"
                min="0"
                value={formData.stock}
                onChange={handleChange}
                className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1 flex items-center gap-1.5">
              <ImageIcon size={14} /> Đường dẫn ảnh (Image URL — Cloudflare R2)
            </label>
            <input
              type="text"
              name="imageUrl"
              value={formData.imageUrl}
              onChange={handleChange}
              placeholder="https://cdn... (hoặc bấm nút Tải ảnh lên CDN bên dưới)"
              className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
            <MediaUploadField
              accept="image/*"
              folder="products/images"
              label="Tải ảnh lên Cloudflare R2"
              onUploaded={(url) => setFormData(prev => ({ ...prev, imageUrl: url }))}
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1 flex items-center gap-1.5">
              <Video size={14} /> Đường dẫn video 4K (Video URL — Cloudflare R2)
            </label>
            <input
              type="text"
              name="videoUrl"
              value={formData.videoUrl}
              onChange={handleChange}
              placeholder="https://cdn... (định dạng mp4/webm)"
              className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500"
            />
            <MediaUploadField
              accept="video/*"
              folder="products/videos"
              label="Tải video lên Cloudflare R2"
              onUploaded={(url) => setFormData(prev => ({ ...prev, videoUrl: url }))}
            />
          </div>

          {(formData.imageUrl || formData.videoUrl) && (
            <div className="p-4 bg-slate-50 rounded-xl border border-gray-200">
              <p className="text-xs font-bold text-gray-600 mb-3 flex items-center gap-1.5">
                <Sparkles size={14} className="text-amber-500" /> Xem trước Media sản phẩm:
              </p>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {formData.imageUrl && (
                  <div className="bg-white rounded-lg p-3 border border-gray-200 flex flex-col items-center">
                    <span className="text-[11px] font-bold text-gray-500 mb-2">Ảnh xem trước</span>
                    <div className="w-full h-36 bg-gray-100 rounded overflow-hidden flex items-center justify-center">
                      <img
                        src={getValidMediaUrl(formData.imageUrl)}
                        alt="Preview"
                        className="w-full h-full object-contain"
                        onError={(e) => {
                          (e.target as HTMLImageElement).src = getValidMediaUrl('/HINH_TRANG_CHU/ipwworlkd.png');
                        }}
                      />
                    </div>
                  </div>
                )}
                {formData.videoUrl && (
                  <div className="bg-white rounded-lg p-3 border border-gray-200 flex flex-col items-center">
                    <span className="text-[11px] font-bold text-gray-500 mb-2">Video xem trước</span>
                    <div className="w-full h-36 bg-black rounded overflow-hidden flex items-center justify-center">
                      <video
                        src={getValidMediaUrl(formData.videoUrl)}
                        controls
                        className="w-full h-full object-contain"
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1">Mô tả sản phẩm</label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows={3}
              className="w-full p-2.5 text-xs sm:text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
            />
          </div>

          <div className="flex flex-col sm:flex-row justify-between items-center gap-3 pt-4 border-t border-gray-100">
            <Link
              href="/admin#daily-revenue"
              className="text-xs font-bold text-emerald-700 hover:text-emerald-800 hover:underline flex items-center gap-1"
            >
              <TrendingUp size={15} /> Trở về coi doanh thu chi tiết theo ngày
            </Link>

            <div className="flex items-center space-x-3 w-full sm:w-auto">
              <button
                type="button"
                onClick={() => router.back()}
                className="flex-1 sm:flex-none px-4 py-2 border border-gray-300 text-gray-700 text-xs font-bold rounded-lg hover:bg-gray-50"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={loading}
                className="flex-1 sm:flex-none px-5 py-2 bg-amber-600 text-white text-xs font-bold rounded-lg hover:bg-amber-700 disabled:bg-gray-400 flex items-center justify-center gap-1.5 shadow-sm"
              >
                <Check size={16} /> {loading ? "Đang lưu..." : "Lưu Thay Đổi"}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
