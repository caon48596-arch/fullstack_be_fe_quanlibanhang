/**
 * k6 Load Test: Kiểm chứng Concurrency Control (Pessimistic Write Lock + Deadlock Prevention)
 * và Idempotency khi 50 người dùng đồng thời mua 1 sản phẩm giới hạn tồn kho (VD: stock = 10).
 *
 * Cách chạy:
 *   k6 run -e BASE_URL=http://localhost:8081/api -e TOKEN=<JWT_ACCESS_TOKEN> -e PRODUCT_ID=1 load-tests/checkout-concurrency.js
 *
 * Kết quả kỳ vọng (khi sản phẩm có stock = 10):
 *   - Đúng 10 request trả HTTP 200 (đặt hàng thành công)
 *   - Đúng 40 request trả HTTP 400 (hết hàng)
 *   - 0 request trả HTTP 500 (không Deadlock, không lỗi hệ thống)
 *   - Tồn kho cuối cùng trong MySQL = 0 (tuyệt đối không âm kho / overselling)
 */

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const successfulOrders = new Counter('checkout_orders_succeeded');
const outOfStockRejections = new Counter('checkout_out_of_stock');
const serverErrors = new Counter('checkout_server_errors');

export const options = {
  scenarios: {
    flash_sale_burst: {
      executor: 'per-vu-iterations',
      vus: 50,
      iterations: 1,
      maxDuration: '30s',
    },
  },
  thresholds: {
    checkout_server_errors: ['count==0'],
    http_req_duration: ['p(95)<1500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081/api';
const TOKEN = __ENV.TOKEN || '';
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || 1);
const BUYER_EMAIL = __ENV.BUYER_EMAIL || 'client@gmail.com';

export default function () {
  const idempotencyKey = `k6-vu-${__VU}-iter-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    email: BUYER_EMAIL,
    items: [
      {
        product: { id: PRODUCT_ID },
        quantity: 1,
      },
    ],
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${TOKEN}`,
      'Idempotency-Key': idempotencyKey,
    },
  };

  const res = http.post(`${BASE_URL}/orders`, payload, params);

  if (res.status === 200) {
    successfulOrders.add(1);
  } else if (res.status === 400 || res.status === 409) {
    outOfStockRejections.add(1);
  } else if (res.status >= 500) {
    serverErrors.add(1);
  }

  check(res, {
    'no 5xx server error (0 deadlock)': (r) => r.status < 500,
    'either 200 OK or 400/409 stock rejected': (r) => r.status === 200 || r.status === 400 || r.status === 409,
    'has X-Request-Id tracing header': (r) => Boolean(r.headers['X-Request-Id']),
  });
}
