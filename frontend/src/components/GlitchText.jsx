import { useEffect, useState } from "react";

const CLIP_PATHS = [
  "inset(10% 0 85% 0)",
  "inset(45% 0 40% 0)",
  "inset(80% 0 5% 0)",
  "inset(10% 0 60% 0)",
  "inset(70% 0 20% 0)",
  "inset(25% 0 50% 0)",
  "inset(55% 0 35% 0)",
  "inset(5% 0 75% 0)",
  "inset(90% 0 2% 0)",
  "inset(30% 0 55% 0)",
  "inset(15% 0 70% 0)",
  "inset(65% 0 25% 0)",
  "inset(40% 0 45% 0)",
  "inset(85% 0 10% 0)",
  "inset(20% 0 65% 0)",
  "inset(50% 0 30% 0)",
  "inset(75% 0 15% 0)",
  "inset(35% 0 52% 0)",
  "inset(60% 0 28% 0)",
  "inset(8% 0 82% 0)",
];

const GlitchText = ({
  children,
  speed = 0.5,
  enableShadows = true,
  enableOnHover = false,
  className = "",
}) => {
  const [isHovered, setIsHovered] = useState(false);
  const [frame, setFrame] = useState(0);

  const shouldAnimate = enableOnHover ? isHovered : true;

  useEffect(() => {
    if (!shouldAnimate) {
      return undefined;
    }

    const intervalMs = Math.max(16, (speed * 1000) / CLIP_PATHS.length);
    const interval = setInterval(() => {
      setFrame((currentFrame) => (currentFrame + 1) % CLIP_PATHS.length);
    }, intervalMs);

    return () => clearInterval(interval);
  }, [shouldAnimate, speed]);

  const containerStyle = {
    position: "relative",
    display: "inline-block",
    userSelect: "none",
    cursor: enableOnHover ? "pointer" : "default",
  };

  const textStyle = {
    position: "relative",
    fontSize: "inherit",
    fontWeight: "inherit",
    lineHeight: "inherit",
    color: "inherit",
  };

  const layerBaseStyle = {
    position: "absolute",
    top: 0,
    left: 0,
    width: "100%",
    height: "100%",
    fontSize: "inherit",
    fontWeight: "inherit",
    lineHeight: "inherit",
    color: "inherit",
    overflow: "hidden",
    willChange: "clip-path, transform",
  };

  const afterIndex = frame;
  const beforeIndex = (frame + 10) % CLIP_PATHS.length;
  const showLayers = enableOnHover ? isHovered : true;

  const afterStyle = {
    ...layerBaseStyle,
    transform: "translateX(8px)",
    textShadow: enableShadows ? "-4px 0 #ff2b6a" : "none",
    clipPath: showLayers ? CLIP_PATHS[afterIndex] : "inset(0 0 100% 0)",
    opacity: showLayers ? 1 : 0,
    transition: "opacity 0.12s",
  };

  const beforeStyle = {
    ...layerBaseStyle,
    transform: "translateX(-8px)",
    textShadow: enableShadows ? "4px 0 #31d7ff" : "none",
    clipPath: showLayers ? CLIP_PATHS[beforeIndex] : "inset(0 0 100% 0)",
    opacity: showLayers ? 1 : 0,
    transition: "opacity 0.12s",
  };

  return (
    <span
      className={className}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onFocus={() => setIsHovered(true)}
      onBlur={() => setIsHovered(false)}
      style={containerStyle}
    >
      <span style={textStyle}>{children}</span>
      <span aria-hidden="true" style={beforeStyle}>
        {children}
      </span>
      <span aria-hidden="true" style={afterStyle}>
        {children}
      </span>
    </span>
  );
};

export default GlitchText;
