// ============================================================
// FILE: api.js
// PURPOSE: Poori application ke liye ek CENTRAL HTTP manager
//          Har API call yahan se hoti hai — ek jagah sab manage
// ============================================================


// ─────────────────────────────────────────────────────────────
// STEP 1: AXIOS IMPORT
// Axios ek JavaScript library hai jo HTTP requests karne mein
// help karti hai (jaise Java mein FeignClient tha — same concept)
// ─────────────────────────────────────────────────────────────
import axios from 'axios';


// ─────────────────────────────────────────────────────────────
// STEP 2: BASE URL DECIDE KARNA
//
// process.env.REACT_APP_API_URL → Environment variable se URL lo
//   Development mein: koi value nahi (undefined)
//   Production mein:  "https://api.mywebsite.com" (set hogi)
//
// || 'http://localhost:8090' → Agar env variable nahi hai,
//   toh DEFAULT localhost use karo (hamare API Gateway ka port)
//
// Result:
//   Local development → http://localhost:8090
//   Production        → environment mein jo set hai woh
// ─────────────────────────────────────────────────────────────
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8090';


// ─────────────────────────────────────────────────────────────
// STEP 3: CUSTOM AXIOS INSTANCE BANANA
//
// axios.create() → Ek naya custom axios object banata hai
// Isme hum DEFAULT SETTINGS dete hain jo har request mein
// automatically apply hongi — baar baar likhne ki zaroorat nahi
// ─────────────────────────────────────────────────────────────
const api = axios.create({

  // baseURL → Har request mein yeh URL automatically prefix hogi
  // Example: api.get('/api/auth/login')
  //    → Internally: GET http://localhost:8090/api/auth/login
  baseURL: API_BASE_URL,

  // headers → Har request ke saath yeh headers automatically jayenge
  // 'Content-Type': 'application/json' → "Main JSON data bhej raha hoon"
  // Backend (Spring Boot) ko pata chalta hai request ka format kya hai
  headers: {
    'Content-Type': 'application/json',
  },

  // timeout → Agar server 15 seconds (15000ms) mein response na de
  // toh automatically error throw karo — user indefinitely wait nahi karega
  timeout: 15000,
});


// ─────────────────────────────────────────────────────────────
// STEP 4: REQUEST INTERCEPTOR
//
// Interceptor = "Beech mein rokne wala"
//
// Yeh interceptor har request JAANE SE PEHLE chalta hai
// Kaam: localStorage se JWT token nikal ke request mein attach karna
// ─────────────────────────────────────────────────────────────
api.interceptors.request.use(

  // SUCCESS function → Normal request ke saath yeh chalega
  // 'config' → request ki saari settings (URL, headers, body, etc.)
  (config) => {

    // localStorage → Browser ka small storage
    // Login ke time token yahan save kiya gaya tha:
    //   localStorage.setItem('sms_token', 'eyJhbGc...')
    // Ab woh token nikal rahe hain:
    const token = localStorage.getItem('sms_token');

    // Agar token hai (matlab user login kiya hua hai)
    // toh request ke header mein attach karo
    if (token) {
      // Authorization header set kar rahe hain
      // Format: "Bearer eyJhbGciOiJIUzI1NiJ9..."
      // "Bearer" → JWT token ke liye standard prefix (protocol convention)
      // Backend (Spring Security) is header ko padhega aur verify karega
      config.headers.Authorization = `Bearer ${token}`;
    }

    // ZAROORI: Modified config wapas return karo
    // Nahi kiya toh request cancel ho jaayegi!
    return config;
  },

  // ERROR function → Request setup mein hi koi error aa jaye (rare case)
  // Promise.reject() → Error ko aage pass karo, calling code handle karega
  (error) => {
    return Promise.reject(error);
  }
);


// ─────────────────────────────────────────────────────────────
// STEP 5: RESPONSE INTERCEPTOR
//
// Yeh interceptor response AANE KE BAAD chalta hai
// Kaam: 401 Unauthorized error handle karna
//
// 401 kab aata hai?
//   - JWT Token expire ho gaya (24 hours baad)
//   - Token invalid/tampered hai
//   - Token nahi bheja gaya
//
// Is case mein: user ko logout karo aur login page pe bhejo
// ─────────────────────────────────────────────────────────────
api.interceptors.response.use(

  // SUCCESS function → Response theek se aaya (200, 201, etc.)
  // As-is wapas do — koi modification nahi
  (response) => response,

  // ERROR function → Response mein koi error aaya
  (error) => {

    // Check karo: kya yeh 401 Unauthorized error hai?
    // error.response        → server ne response diya (lekin error ke saath)
    // error.response.status → HTTP status code (401, 404, 500, etc.)
    if (error.response && error.response.status === 401) {

      // Token expire ho gaya → purana data clear karo
      // Login ke time teen cheezein localStorage mein save ki thi:
      localStorage.removeItem('sms_token'); // JWT token hatao
      localStorage.removeItem('sms_user');  // Username hatao
      localStorage.removeItem('sms_role');  // Role (ADMIN/STUDENT) hatao

      // Ab user ko login page pe bhejo
      // window.location.pathname → abhi kaun sa page open hai
      // Check zaroor karo: agar already '/login' pe hai toh redirect mat karo
      // (Nahi check kiya toh: login → 401 → redirect → login → 401 → loop!)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'; // Forcefully login page pe le jao
      }
    }

    // Error ko aage pass karo
    // Calling code (component) bhi apne hisaab se handle kar sake
    return Promise.reject(error);
  }
);


// ─────────────────────────────────────────────────────────────
// STEP 6: EXPORT
// Is 'api' instance ko export karo taaki doosri files use kar sakein
//
// Usage in other files:
//   import api from './api';
//   const response = await api.post('/api/auth/login', data);
//   → Automatically:
//     ✅ baseURL lagega (http://localhost:8090)
//     ✅ Content-Type header lagega
//     ✅ JWT token attach hoga (agar hai)
//     ✅ 401 handle hoga (auto logout)
// ─────────────────────────────────────────────────────────────
export default api;


// ─────────────────────────────────────────────────────────────
// SUMMARY: YEH FILE KYA KARTI HAI
//
//  1. Axios instance banati hai (baseURL, headers, timeout)
//  2. Request Interceptor:
//       Har request se PEHLE → localStorage se token nikalo
//                             → Authorization header mein lagao
//  3. Response Interceptor:
//       Har response KE BAAD → 401 aaya? → localStorage clear karo
//                                         → /login pe redirect karo
//  4. Export → Poori app yeh ek instance use karti hai
//
// FAYDA:
//   Ek jagah setup → Sab jagah kaam karta hai
//   Koi bhi component mein manually token attach nahi karna padta
//   Koi bhi component mein manually 401 handle nahi karna padta
// ─────────────────────────────────────────────────────────────