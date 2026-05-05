const normalizeUserId = (userId) => String(userId);

const normalizeRoleList = (roles) => {
  if (Array.isArray(roles)) {
    return [...new Set(roles.map((role) => String(role || "").trim()).filter(Boolean))];
  }

  const singleRole = String(roles || "").trim();
  return singleRole ? [singleRole] : [];
};

export { normalizeRoleList, normalizeUserId };