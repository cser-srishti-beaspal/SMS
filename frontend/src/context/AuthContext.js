import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
// createContext  → Global state banane ke liye
// useState       → user aur loading state manage karne ke liye
// useContext     → Context consume karne ke liye (useAuth mein use hoga)
// useEffect      → Page refresh pe localStorage se user restore karne ke liye
// useCallback    → Functions ko unnecessary re-create hone se bachane ke liye

// ─────────────────────────────────────────────────────────────
// AuthContext — Poori app ka authentication state yahan rehta hai
//
// Yeh file teen kaam karti hai:
//   1. User ko login/logout karna
//   2. Page refresh pe user ko restore karna (localStorage se)
//   3. Role check karna — ADMIN hai ya STUDENT
//
// Koi bhi component useAuth() hook se yeh sab access kar sakta hai
// ─────────────────────────────────────────────────────────────
const AuthContext = createContext(null); // null = default value jab Provider ke bahar use ho


// ── AuthProvider ───────────────────────────────────────────────
// Yeh poori app ko wrap karta hai (main index.js mein)
// children = jo bhi is Provider ke andar hai woh sab
export const AuthProvider = ({ children }) => {

  // user = { token, username, role } — null matlab logged out
  const [user, setUser] = useState(null);

  // loading = true jab tak localStorage check nahi ho jaata
  // Bina loading ke — refresh pe briefly logged-out dikhta hai (flicker)
  const [loading, setLoading] = useState(true);


  // ── Page Refresh Pe User Restore Karo ─────────────────────────
  // [] dependency = sirf ek baar chalega — component mount hone par
  // localStorage mein token/user/role saved hai → wapas state mein laao
  useEffect(() => {
    try {
      const token    = localStorage.getItem('sms_token');
      const username = localStorage.getItem('sms_user');
      const role     = localStorage.getItem('sms_role');

      // Teeno hain tabhi user restore karo — ek bhi missing toh guest samjho
      if (token && username && role) {
        setUser({ token, username, role });
      }
    } catch (err) {
      // localStorage corrupt ho sakta hai — safe rehne ke liye clear karo
      console.error('Error restoring auth state:', err);
      localStorage.removeItem('sms_token');
      localStorage.removeItem('sms_user');
      localStorage.removeItem('sms_role');
    } finally {
      // Try succeed ho ya fail — loading hamesha false karo
      // Warna app hamesha spinner dikhata rahega
      setLoading(false);
    }
  }, []);


  // ── Login ──────────────────────────────────────────────────────
  // Backend se token + username + role milne ke baad yeh call hota hai
  // useCallback: login function tab tak re-create nahi hoga jab tak dependency na badle
  //              Performance optimization — child components unnecessarily re-render nahi hote
  const login = useCallback((token, username, role) => {

    // Backend role alag formats mein bhej sakta hai: "ROLE_ADMIN" ya "ADMIN"
    // Normalize karo: hamesha "ADMIN" ya "STUDENT" store karo
    // "ROLE_ADMIN".replace('ROLE_', '') → "ADMIN"
    // "ADMIN".replace('ROLE_', '')      → "ADMIN" (kuch nahi badla)
    const normalizedRole = role ? role.toUpperCase().replace('ROLE_', '') : 'STUDENT';

    // localStorage mein save karo — page refresh pe yahi kaam aayega
    localStorage.setItem('sms_token', token);
    localStorage.setItem('sms_user', username);
    localStorage.setItem('sms_role', normalizedRole);

    // React state update karo — UI turant reflect karega
    setUser({ token, username, role: normalizedRole });
  }, []); // [] = koi dependency nahi, ek baar banta hai


  // ── Logout ─────────────────────────────────────────────────────
  // localStorage saaf karo + state null karo
  // Dono zaroori hain: sirf state null karne se refresh pe wapas logged in ho jaayega
  const logout = useCallback(() => {
    localStorage.removeItem('sms_token');
    localStorage.removeItem('sms_user');
    localStorage.removeItem('sms_role');
    setUser(null); // UI turant logged-out state dikhaayega
  }, []);


  // ── Role Check Helpers ─────────────────────────────────────────
  // user? = optional chaining — user null ho toh crash nahi, undefined milega
  // Dono formats check karte hain kyunki normalize karne ke baad bhi
  // kabhi kabhi "ROLE_ADMIN" aa sakta hai

  // Kya logged-in user ADMIN hai?
  const isAdmin = useCallback(() => {
    return user?.role === 'ADMIN' || user?.role === 'ROLE_ADMIN';
  }, [user]); // user badle tabhi recalculate karo

  // Kya logged-in user STUDENT hai?
  const isStudent = useCallback(() => {
    return user?.role === 'STUDENT' || user?.role === 'ROLE_STUDENT';
  }, [user]);

  // Kya koi user logged in hai?
  // !! = double negation → value ko boolean mein convert karo
  // user?.token = "eyJhb..." → !! → true
  // user?.token = undefined  → !! → false
  const isAuthenticated = useCallback(() => {
    return !!user?.token;
  }, [user]);


  // ── Context Value ──────────────────────────────────────────────
  // Yeh sab cheezein poori app mein useAuth() se milegi
  const value = {
    user,            // { token, username, role } ya null
    loading,         // true jab tak localStorage check na ho
    login,           // fn: login ke baad call karo
    logout,          // fn: logout button pe call karo
    isAdmin,         // fn: ADMIN check
    isStudent,       // fn: STUDENT check
    isAuthenticated, // fn: koi bhi logged in hai?
  };

  // Provider ke andar jo bhi children hain unhe yeh value milegi
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};


// ── useAuth Hook ───────────────────────────────────────────────
// Koi bhi component mein likho: const { user, login, logout } = useAuth();
// AuthProvider ke bahar use kiya toh error throw karo — misuse se bachao
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;