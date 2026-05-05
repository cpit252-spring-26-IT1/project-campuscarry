import { Button, Chip } from "@heroui/react";
import { PRIMARY_BUTTON_CLASS } from "./adminShared";

const IMAGE_MIN_ZOOM = 1;

const ProofImageViewer = ({
  activeProofImage,
  changeZoom,
  closeImageViewer,
  imagePan,
  imageZoom,
  isArabic,
  isPanningImage,
  onPointerDown,
  onPointerMove,
  onWheel,
  resetImageViewer,
  stopImagePanning,
  zoomStep,
}) => {
  if (!activeProofImage) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-[120] bg-black/75 p-4 backdrop-blur-sm" onClick={closeImageViewer}>
      <div className="mx-auto flex h-full w-full max-w-6xl flex-col" onClick={(event) => event.stopPropagation()}>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-xl border border-[#cae9ea]/25 bg-[#1d1d1d]/85 px-4 py-3">
          <div>
            <p className="cc-title-card text-[#cae9ea]">
              {isArabic ? "معاينة إثبات الرتبة" : "Rank Proof Viewer"}
            </p>
            <p className="text-xs text-[#cae9ea]/75">
              {activeProofImage.username ? `@${activeProofImage.username}` : ""}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Chip variant="flat" className="bg-[#208c8c]/20 text-[#cae9ea]">
              {Math.round(imageZoom * 100)}%
            </Chip>
            <Button
              size="sm"
              className="cc-button-text border border-[#3c4748]/60 bg-transparent text-[#cae9ea] hover:bg-[#273b40]"
              onPress={() => changeZoom(-zoomStep)}
            >
              {isArabic ? "تصغير" : "Zoom Out"}
            </Button>
            <Button
              size="sm"
              className="cc-button-text border border-[#3c4748]/60 bg-transparent text-[#cae9ea] hover:bg-[#273b40]"
              onPress={resetImageViewer}
            >
              {isArabic ? "إعادة ضبط" : "Reset"}
            </Button>
            <Button
              size="sm"
              className="cc-button-text border border-[#3c4748]/60 bg-transparent text-[#cae9ea] hover:bg-[#273b40]"
              onPress={() => changeZoom(zoomStep)}
            >
              {isArabic ? "تكبير" : "Zoom In"}
            </Button>
            <Button size="sm" className={PRIMARY_BUTTON_CLASS} onPress={closeImageViewer}>
              {isArabic ? "إغلاق" : "Close"}
            </Button>
          </div>
        </div>

        <div
          className={`relative flex-1 overflow-hidden rounded-xl border border-[#cae9ea]/25 bg-black/45 ${
            imageZoom > IMAGE_MIN_ZOOM ? (isPanningImage ? "cursor-grabbing" : "cursor-grab") : "cursor-zoom-in"
          }`}
          onWheel={onWheel}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={stopImagePanning}
          onPointerLeave={stopImagePanning}
          onPointerCancel={stopImagePanning}
        >
          <img
            src={activeProofImage.src}
            alt={isArabic ? "إثبات الرتبة" : "Rank proof"}
            draggable={false}
            className="h-full w-full select-none object-contain"
            style={{
              transform: `translate(${imagePan.x}px, ${imagePan.y}px) scale(${imageZoom})`,
              transition: isPanningImage ? "none" : "transform 120ms ease-out",
              transformOrigin: "center center",
            }}
          />
        </div>

        <p className="mt-2 text-xs text-[#cae9ea]/80">
          {isArabic
            ? "استخدم عجلة الفأرة للتكبير واسحب الصورة للتحريك. اضغط Esc للإغلاق."
            : "Use mouse wheel to zoom and drag to move around. Press Esc to close."}
        </p>
      </div>
    </div>
  );
};

export default ProofImageViewer;
