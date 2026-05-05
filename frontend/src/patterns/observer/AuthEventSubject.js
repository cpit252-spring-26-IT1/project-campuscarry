import Subject from "./Subject";

export const AUTH_EVENT_TYPES = {
  LOGIN: "AUTH_LOGIN",
  LOGOUT: "AUTH_LOGOUT",
  REGISTER: "AUTH_REGISTER",
};

class AuthEventSubject extends Subject {
  emitLogin(user) {
    this.notify({
      type: AUTH_EVENT_TYPES.LOGIN,
      occurredAt: new Date().toISOString(),
      userId: user?.id != null ? String(user.id) : null,
      role: user?.role || null,
    });
  }

  emitLogout(user) {
    this.notify({
      type: AUTH_EVENT_TYPES.LOGOUT,
      occurredAt: new Date().toISOString(),
      userId: user?.id != null ? String(user.id) : null,
      role: user?.role || null,
    });
  }

  emitRegister(role) {
    this.notify({
      type: AUTH_EVENT_TYPES.REGISTER,
      occurredAt: new Date().toISOString(),
      role: role || null,
    });
  }
}

const authEventSubject = new AuthEventSubject();

export default authEventSubject;
