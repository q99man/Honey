const KAKAO_MAP_SCRIPT_ID = "kakao-map-sdk";

export function getKakaoMapJavaScriptKey() {
  const key = import.meta.env.VITE_KAKAO_MAP_JAVASCRIPT_KEY as
    | string
    | undefined;
  const legacyKey = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY as
    | string
    | undefined;

  return key?.trim() || legacyKey?.trim() || "";
}

export function loadKakaoMapSdk(appKey: string) {
  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao);
  }

  return new Promise<KakaoWindow>((resolve, reject) => {
    const existingScript = document.getElementById(KAKAO_MAP_SCRIPT_ID);
    if (existingScript) {
      existingScript.addEventListener("load", () => {
        window.kakao?.maps.load(() => resolve(window.kakao!));
      });
      existingScript.addEventListener("error", reject);
      return;
    }

    const script = document.createElement("script");
    script.id = KAKAO_MAP_SCRIPT_ID;
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(
      appKey,
    )}&autoload=false`;
    script.onload = () => {
      window.kakao?.maps.load(() => resolve(window.kakao!));
    };
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

export type KakaoWindow = {
  maps: {
    load: (callback: () => void) => void;
    LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
    LatLngBounds: new () => KakaoLatLngBounds;
    Map: new (
      container: HTMLElement,
      options: { center: KakaoLatLng; level: number },
    ) => KakaoMap;
    Marker: new (options: {
      map: KakaoMap;
      position: KakaoLatLng;
      title?: string;
    }) => KakaoMarker;
    event: {
      addListener: (
        target: KakaoMarker,
        type: "click",
        handler: () => void,
      ) => void;
    };
  };
};

export type KakaoLatLng = object;

export type KakaoLatLngBounds = {
  extend: (latLng: KakaoLatLng) => void;
};

export type KakaoMap = {
  setBounds: (bounds: KakaoLatLngBounds) => void;
  setCenter: (latLng: KakaoLatLng) => void;
};

export type KakaoMarker = {
  setMap: (map: KakaoMap | null) => void;
};

declare global {
  interface Window {
    kakao?: KakaoWindow;
  }
}
