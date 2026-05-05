const getSelectValue = (keys) => {
  if (keys === "all") {
    return "";
  }

  const [value] = Array.from(keys);
  return value ? String(value) : "";
};

const getSelectValues = (keys) => {
  if (keys === "all") {
    return [];
  }

  return Array.from(keys).map((value) => String(value));
};

export { getSelectValue, getSelectValues };