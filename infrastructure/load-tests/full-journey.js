import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');
export let browseTrend = new Trend('browse_duration');
export let authTrend = new Trend('auth_duration');
export let bookTrend = new Trend('book_duration');
export let payTrend = new Trend('pay_duration');

// Gateway Base URL - ALL traffic must go through the Gateway in production
const BASE_URL = 'http://localhost:8080';

// Options for a realistic full journey load test
export let options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp up to 50 concurrent users
    { duration: '2m', target: 100 },   // Sustain at 100 users for 2 mins (Normal Load)
    { duration: '30s', target: 200 },  // Quick spike to 200 users (Stress Spike)
    { duration: '30s', target: 0 }     // Ramp down gracefully
  ],
  thresholds: {
    'errors': ['rate<0.05'],           // 95% overall success rate (factoring concurrency conflicts)
    'http_req_duration': ['p(95)<1500'] // 95% of all requests under 1.5s
  }
};

function randomUser() {
  const id = __VU + '-' + __ITER + '-' + Math.floor(Math.random() * 1000000);
  return {
    email: `user${id}@ticketblitz.com`,
    password: 'Password123!',
    firstName: 'Test',
    lastName: 'User'
  };
}

export default function () {
  // ----------------------------------------------------
  // STEP 1: BROWSE EVENTS
  // ----------------------------------------------------
  let resBrowse = http.get(`${BASE_URL}/api/v1/events/upcoming`);
  check(resBrowse, { 'Events loaded': r => r.status === 200 });
  browseTrend.add(resBrowse.timings.duration);
  if (resBrowse.status !== 200) errorRate.add(1);
  
  // Think time: read the events
  sleep(Math.random() * 2 + 1); // 1-3 seconds

  // ----------------------------------------------------
  // STEP 2: AUTHENTICATE (Register + Login)
  // ----------------------------------------------------
  const user = randomUser();
  let resReg = http.post(`${BASE_URL}/auth/register`, JSON.stringify(user), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(resReg, { 'Registered successfully': r => r.status === 200 });
  if (resReg.status !== 200) errorRate.add(1);
  
  sleep(0.5);

  let resLogin = http.post(`${BASE_URL}/auth/login`, JSON.stringify({ email: user.email, password: user.password }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(resLogin, { 'Logged in successfully': r => r.status === 200 });
  authTrend.add(resLogin.timings.duration);
  if (resLogin.status !== 200) {
    errorRate.add(1);
    return; // Cannot proceed without token
  }
  
  const token = resLogin.json('data.accessToken');
  if (!token) return;

  // Think time: deciding which seats to pick
  sleep(Math.random() * 1 + 0.5);

  // ----------------------------------------------------
  // STEP 3: BOOK SEATS (Event 1 has seats 1 to 650)
  // ----------------------------------------------------
  // Pick 2 random seats to book
  const seatId1 = Math.floor(Math.random() * 650) + 1;
  const seatId2 = Math.floor(Math.random() * 650) + 1;
  const idempotencyKey = `booking-${user.email}-${__ITER}`;

  const bookingPayload = JSON.stringify({
    eventId: 1,
    seatIds: [seatId1, seatId2],
    idempotencyKey: idempotencyKey
  });

  let resBook = http.post(`${BASE_URL}/api/v1/bookings`, bookingPayload, {
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
  });

  bookTrend.add(resBook.timings.duration);
  
  // 409 means seats taken/locked (expected in high concurrency). 200 means success.
  check(resBook, { 'Booking success or expected conflict': r => r.status === 200 || r.status === 409 });
  
  if (resBook.status !== 200) {
    // Expected concurrency conflict, just end the journey here rather than failing
    if (resBook.status !== 409) errorRate.add(1);
    return;
  }

  const bookingId = resBook.json('data.id');
  
  // Think time: enter credit card details
  sleep(Math.random() * 2 + 1);

  // ----------------------------------------------------
  // STEP 4: PAY FOR BOOKING
  // ----------------------------------------------------
  const paymentPayload = JSON.stringify({
    paymentMethod: 'CREDIT_CARD',
    cardNumber: '4242424242424242'
  });

  let resPay = http.post(`${BASE_URL}/api/v1/bookings/${bookingId}/payment`, paymentPayload, {
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
  });

  payTrend.add(resPay.timings.duration);
  check(resPay, { 'Payment processed': r => r.status === 200 });
  if (resPay.status !== 200) errorRate.add(1);

  // Journey Complete!
  sleep(1);
}
