import { useState } from 'react';
import Section from '../components/Section';

export default function AuthPanel({ auth }) {
  const [registerForm, setRegisterForm] = useState({ fullName: '', email: '', password: '' });
  const [loginForm, setLoginForm] = useState({ email: '', password: '' });
  const [forgotEmail, setForgotEmail] = useState('');
  const [resetForm, setResetForm] = useState({ token: '', newPassword: '' });
  const [changeForm, setChangeForm] = useState({ currentPassword: '', newPassword: '' });
  const [message, setMessage] = useState({ ok: true, text: '' });

  const run = async (fn) => {
    const rs = await fn();
    setMessage({ ok: rs.ok, text: rs.message });
  };

  return (
    <>
      <Section title="Authentication Overview">
        <p className="muted">Backend URL: <code>{import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}</code></p>
        <p>{auth.token ? `✅ Logged in (${auth.user?.email || 'user'})` : '⚠️ Not logged in'}</p>
        {message.text && <p className={message.ok ? 'status-ok' : 'status-error'}>{message.text}</p>}
      </Section>

      <Section title="Create account">
        <div className="form-grid">
          <div className="col-4"><input placeholder="Full name" value={registerForm.fullName} onChange={(e) => setRegisterForm({ ...registerForm, fullName: e.target.value })} /></div>
          <div className="col-4"><input placeholder="Email" value={registerForm.email} onChange={(e) => setRegisterForm({ ...registerForm, email: e.target.value })} /></div>
          <div className="col-4"><input placeholder="Password" type="password" value={registerForm.password} onChange={(e) => setRegisterForm({ ...registerForm, password: e.target.value })} /></div>
          <div className="col-12"><button disabled={auth.loading} onClick={() => run(() => auth.register(registerForm.fullName, registerForm.email, registerForm.password))}>Register</button></div>
        </div>
      </Section>

      <Section title="Login">
        <div className="form-grid">
          <div className="col-6"><input placeholder="Email" value={loginForm.email} onChange={(e) => setLoginForm({ ...loginForm, email: e.target.value })} /></div>
          <div className="col-6"><input placeholder="Password" type="password" value={loginForm.password} onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })} /></div>
          <div className="col-12 row">
            <button disabled={auth.loading} onClick={() => run(() => auth.login(loginForm.email, loginForm.password))}>Login</button>
            <button className="secondary" onClick={auth.logout}>Logout</button>
          </div>
        </div>
      </Section>

      <Section title="Forgot / Reset / Change password">
        <div className="form-grid">
          <div className="col-4"><input placeholder="Forgot password email" value={forgotEmail} onChange={(e) => setForgotEmail(e.target.value)} /></div>
          <div className="col-3"><button onClick={() => run(() => auth.forgotPassword(forgotEmail))}>Send reset email</button></div>

          <div className="col-3"><input placeholder="Reset token" value={resetForm.token} onChange={(e) => setResetForm({ ...resetForm, token: e.target.value })} /></div>
          <div className="col-3"><input placeholder="New password" type="password" value={resetForm.newPassword} onChange={(e) => setResetForm({ ...resetForm, newPassword: e.target.value })} /></div>
          <div className="col-3"><button onClick={() => run(() => auth.resetPassword(resetForm.token, resetForm.newPassword))}>Reset password</button></div>

          <div className="col-3"><input placeholder="Current password" type="password" value={changeForm.currentPassword} onChange={(e) => setChangeForm({ ...changeForm, currentPassword: e.target.value })} /></div>
          <div className="col-3"><input placeholder="New password" type="password" value={changeForm.newPassword} onChange={(e) => setChangeForm({ ...changeForm, newPassword: e.target.value })} /></div>
          <div className="col-3"><button onClick={() => run(() => auth.changePassword(changeForm.currentPassword, changeForm.newPassword))}>Change password</button></div>
        </div>
      </Section>
    </>
  );
}
