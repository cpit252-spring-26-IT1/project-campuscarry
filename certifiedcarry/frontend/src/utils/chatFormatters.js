const IMAGE_FILE_EXTENSIONS = [".png", ".jpg", ".jpeg", ".gif", ".webp", ".avif", ".bmp", ".svg"];

const formatTime = (value) => {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleString();
};

const isLikelyImageUrl = (value) => {
  const candidate = String(value || "").trim();
  if (!candidate) {
    return false;
  }

  if (candidate.startsWith("data:image/")) {
    return true;
  }

  try {
    const parsed = new URL(candidate);
    if (parsed.protocol !== "https:" && parsed.protocol !== "http:") {
      return false;
    }

    const normalizedPath = String(parsed.pathname || "").toLowerCase();
    return IMAGE_FILE_EXTENSIONS.some((extension) => normalizedPath.endsWith(extension));
  } catch {
    return false;
  }
};

const getMessageDisplayParts = (value) => {
  const lines = String(value || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  let imageUrl = "";
  const textLines = [];

  lines.forEach((line) => {
    if (!imageUrl && isLikelyImageUrl(line)) {
      imageUrl = line;
      return;
    }

    textLines.push(line);
  });

  return {
    text: textLines.join("\n").trim(),
    imageUrl,
  };
};

export { formatTime, getMessageDisplayParts, isLikelyImageUrl };