import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');

const BASE_URL = 'http://localhost:8080';

// Stress test configuration aiming to break a local cluster
export let options = {
  stages: [
    { duration: '30s', target: 200 },   // Warmup to the max we know works
    { duration: '1m', target: 600 },    // Aggressive ramp to 600 VUs
    { duration: '1m', target: 1200 },   // Extreme spike to 1200 VUs (Breaking point target)
    { duration: '30s', target: 0 }      // Collapse
  ]
};

function randomUser() {
  const id = __VU + '-' + __ITER + '-' + Math.floor(Math.random() * 1000000);
  return {
    email: `stress${id}@ticketblitz.com`,
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User'
  };
}

export default function () {
  // Hit the catalog service (read-heavy, cacheable but still hits the DB on miss)
  let resBrowse = http.get(`${BASE_URL}/api/v1/events/upcoming`);
  if (resBrowse.status !== 200) errorRate.add(1);
  
  sleep(Math.random() * 0.5); // Minimal think time = Maximum spam

  // Hit the auth service (write-heavy to Redis + DB)
  const user = randomUser();
  let resReg = http.post(`${BASE_URL}/auth/register`, JSON.stringify(user), {
    headers: { 'Content-Type': 'application/json' },
  });
  if (resReg.status !== 200) errorRate.add(1);

  let resLogin = http.post(`${BASE_URL}/auth/login`, JSON.stringify({ email: user.email, password: user.password }), {
    headers: { 'Content-Type': 'application/json' },
  });
  if (resLogin.status !== 200) {
    errorRate.add(1);
    return;
  }
  
  const token = resLogin.json('data.accessToken');
  if (!token) return;

  // Hit the booking service (transactional writes, locks)
  const seatId1 = Math.floor(Math.random() * 650) + 1;
  const idempotencyKey = `stress-booking-${user.email}-${__ITER}`;

  let resBook = http.post(`${BASE_URL}/api/v1/bookings`, JSON.stringify({
    eventId: 1,
    seatIds: [seatId1],
    idempotencyKey: idempotencyKey
  }), {
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
  });
  
  if (resBook.status !== 200 && resBook.status !== 409) errorRate.add(1);
}
