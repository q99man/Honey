const fs = require('fs');
let code = fs.readFileSync('src/pages/HomePage.tsx', 'utf8');

// 2. Add new close button to ImagePane
const oldImageBtn = `      <button
        type="button"
        onClick={handleImageClick}
        className={full ? "relative block shrink-0 px-2 pb-3 text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-[#f6b800]/45" : "relative block min-h-0 flex-1 basis-1/2 px-2 pb-0 text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-[#f6b800]/45"}
        aria-label="사진 탭 보기"
      >
        <div
          className={
            full
              ? "grid h-[202px] grid-cols-[1.05fr_1fr_0.82fr] gap-2 overflow-hidden rounded-[18px] bg-[#fff1bf]"
              : "grid h-full min-h-[150px] grid-cols-3 gap-px overflow-hidden rounded-[16px] bg-[#fff1bf]"
          }
        >
          <PlaceImagePane place={place} className="h-full" />
          <PlaceImagePane place={place} className="h-full brightness-[0.98]" />
          <PlaceImagePane place={place} className="h-full" />
        </div>
        <span className="absolute bottom-6 left-1/2 -translate-x-1/2 rounded-full bg-black/55 px-3 py-1 text-xs font-bold text-white">
          1/6
        </span>
      </button>`;

const newImageBtn = `      <div className={full ? "relative block shrink-0 px-2 pb-3 text-left" : "relative block min-h-0 flex-1 basis-1/2 px-2 pb-0 text-left"}>
        <button
          type="button"
          onClick={handleImageClick}
          className="block h-full w-full text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-[#f6b800]/45"
          aria-label="사진 탭 보기"
        >
          <div
            className={
              full
                ? "grid h-[202px] grid-cols-[1.05fr_1fr_0.82fr] gap-2 overflow-hidden rounded-[18px] bg-[#fff1bf]"
                : "grid h-full min-h-[150px] grid-cols-3 gap-px overflow-hidden rounded-[16px] bg-[#fff1bf]"
            }
          >
            <PlaceImagePane place={place} className="h-full" />
            <PlaceImagePane place={place} className="h-full brightness-[0.98]" />
            <PlaceImagePane place={place} className="h-full" />
          </div>
        </button>
        <span className="pointer-events-none absolute bottom-6 left-1/2 -translate-x-1/2 rounded-full bg-black/55 px-3 py-1 text-xs font-bold text-white">
          1/6
        </span>
        <button
          type="button"
          aria-label="맛집 상세 닫기"
          onClick={onClose}
          className="absolute right-4 top-3 flex h-8 w-8 items-center justify-center rounded-full bg-white/90 text-lg font-black text-[#2b210f] shadow-sm backdrop-blur-md transition active:scale-95"
        >
          ×
        </button>
      </div>`;

code = code.replace(oldImageBtn, newImageBtn);
code = code.replace(oldImageBtn.replace(/\n/g, '\r\n'), newImageBtn);


// 3. Update createPlaceOverlay SVG
const oldOverlay = `function createPlaceOverlay(place: Place, active: boolean) {
  const category = getFoodCategory(place.category);
  const root = document.createElement("div");
  root.dataset.placeMarkerRoot = "true";
  root.className = "pointer-events-none flex h-14 w-14 items-center justify-center";
  root.style.pointerEvents = "none";
  root.style.width = "56px";
  root.style.height = "56px";

  const marker = document.createElement("button");
  marker.type = "button";
  marker.title = place.title;
  marker.dataset.placeMarkerHit = "true";
  marker.className = active
    ? "pointer-events-auto flex h-12 w-12 origin-bottom scale-110 items-center justify-center rounded-[20px_20px_20px_7px] border-[3px] border-white bg-[#f35f4f] text-xl text-white shadow-[0_14px_32px_rgba(243,95,79,0.42)] ring-4 ring-[#f6d365]/50 transition active:scale-105"
    : "pointer-events-auto flex h-10 w-10 origin-bottom items-center justify-center rounded-[18px_18px_18px_6px] border-2 border-white bg-[#f6b800] text-lg text-[#2b210f] shadow-[0_10px_24px_rgba(43,33,15,0.30)] ring-2 ring-[#2b210f]/10 transition hover:border-white hover:bg-[#ffd84d] active:scale-95";
  marker.style.pointerEvents = "auto";
  marker.textContent = category.label[0] ?? "맛";

  root.append(marker);
  return root;
}`;

const newOverlay = `function createPlaceOverlay(place: Place, active: boolean) {
  const category = getFoodCategory(place.category);
  const root = document.createElement("div");
  root.dataset.placeMarkerRoot = "true";
  root.className = "pointer-events-none flex items-end justify-center pb-2";
  root.style.pointerEvents = "none";
  root.style.width = "56px";
  root.style.height = "64px";

  const marker = document.createElement("button");
  marker.type = "button";
  marker.title = place.title;
  marker.dataset.placeMarkerHit = "true";
  marker.className = active
    ? "pointer-events-auto flex h-11 w-11 origin-center -rotate-45 scale-110 items-center justify-center rounded-[50%_50%_50%_0] border-[3px] border-white bg-[#f35f4f] text-xl text-white shadow-[-6px_6px_16px_rgba(243,95,79,0.42)] transition-transform active:scale-105"
    : "pointer-events-auto flex h-9 w-9 origin-center -rotate-45 items-center justify-center rounded-[50%_50%_50%_0] border-2 border-white bg-[#f6b800] text-lg text-[#2b210f] shadow-[-4px_4px_12px_rgba(43,33,15,0.25)] transition-transform hover:border-white hover:bg-[#ffd84d] hover:scale-105 active:scale-95";
  marker.style.pointerEvents = "auto";

  const iconSpan = document.createElement("span");
  iconSpan.className = active
    ? "rotate-45 flex h-6 w-6 items-center justify-center"
    : "rotate-45 flex h-5 w-5 items-center justify-center";
  iconSpan.innerHTML = category.iconSvg;

  marker.append(iconSpan);
  root.append(marker);
  return root;
}`;

code = code.replace(oldOverlay, newOverlay);
code = code.replace(oldOverlay.replace(/\n/g, '\r\n'), newOverlay);

// 4. Update map background
code = code.replace('className="pointer-events-auto absolute inset-0 z-0 h-full w-full touch-none select-none overflow-hidden bg-[#eaf2e4]"', 'className="pointer-events-auto absolute inset-0 z-0 h-full w-full touch-none select-none overflow-hidden bg-[#fffaf0]"');
code = code.replace('bg-[#eaf2e4] px-8 text-center', 'bg-[#fffaf0] px-8 text-center');

fs.writeFileSync('src/pages/HomePage.tsx', code);
console.log('Update complete!');
