import { useState } from 'react';
import Section from '../components/Section';

export default function AuthPanel({ auth }) {
  const [registerForm, setRegisterForm] = useState({ fullName: '', email: '', password: '' });
  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [forgotEmail, setForgotEmail] = useState('');
  const [resetForm, setResetForm] = useState({ token: '', newPassword: '' });
  const [changeForm, setChangeForm] = useState({ currentPassword: '', newPassword: '' });
  const [message, setMessage] = useState('');

  const run = async (fn) => {
    const rs = await fn();
    setMessage(`${rs.ok ? '✅' : '❌'} ${rs.message}`);
  };

  return (
    <>
      <Section title="Authentication">
        <small>Backend base URL: <code>{import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}</code></small>
        <p>{auth.token ? `Logged in token present (${auth.token.slice(0, 20)}...)` : 'Not logged in'}</p>
        {message && <p>{message}</p>}
      </Section>

      <Section title="Register">
        <input placeholder="Full name" value={registerForm.fullName} onChange={(e) => setRegisterForm({ ...registerForm, fullName: e.target.value })} />
        <input placeholder="Email" value={registerForm.email} onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })} />
        <input placeholder="Password" type="password" value={registerForm.password} onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })} />
        <button onClick={() => run(() => auth.register(registerForm.fullName, registerForm.email, registerForm.password))}>Register</button>
      </Section>

      <Section title="Login">
        <input placeholder="Email" value={loginForm.email} onChange={(e) => setLoginForm({ ...loginForm, email: e.target.value })} />
        <input placeholder="Password" type="password" value={loginForm.password} onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })} />
        <div className="row">
          <button onClick={() => run(() => auth.login(loginForm.email, loginForm.password))}>Login</button>
          <button onClick={auth.logout}>Logout</button>
        </div>
      </Section>

      <Section title="Forgot Password">
        <input placeholder="Email" value={forgotEmail} onChange={(e) => setForgotEmail(e.target.value)} />
        <button onClick={() => run(() => auth.forgotPassword(forgotEmail))}>Send forgot-password</button>
      </Section>

      <Section title="Reset Password">
        <input placeholder="Token" value={resetForm.token} onChange={(e) => setResetForm({ ...resetForm, token: e.target.value })} />
        <input placeholder="New password" type="password" value={resetForm.newPassword} onChange={(e) => setResetForm({ ...resetForm, newPassword: e.target.value })} />
        <button onClick={() => run(() => auth.resetPassword(resetForm.token, resetForm.newPassword))}>Reset password</button>
      </Section>

      <Section title="Change Password (requires login)">
        <input placeholder="Current password" type="password" value={changeForm.currentPassword} onChange={(e) => setChangeForm({ ...changeForm, currentPassword: e.target.value })} />
        <input placeholder="New password" type="password" value={changeForm.newPassword} onChange={(e) => setChangeForm({ ...changeForm, newPassword: e.target.value })} />
        <button onClick={() => run(() => auth.changePassword(changeForm.currentPassword, changeForm.newPassword))}>Change password</button>
      </Section>
    </>
  );
}
