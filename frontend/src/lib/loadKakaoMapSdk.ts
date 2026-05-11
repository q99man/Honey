const KAKAO_MAP_SCRIPT_ID = "kakao-map-sdk";
let kakaoMapSdkPromise: Promise<KakaoWindow> | null = null;
let kakaoMapSdkReady = false;

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
  if (kakaoMapSdkPromise) {
    return kakaoMapSdkPromise;
  }

  if (kakaoMapSdkReady && window.kakao?.maps) {
    return Promise.resolve(window.kakao);
  }

  if (window.kakao?.maps) {
    kakaoMapSdkPromise = new Promise<KakaoWindow>((resolve) => {
      window.kakao!.maps.load(() => {
        kakaoMapSdkReady = true;
        resolve(window.kakao!);
      });
    });
    return kakaoMapSdkPromise;
  }

  kakaoMapSdkPromise = new Promise<KakaoWindow>((resolve, reject) => {
    const handleError = (error: unknown) => {
      kakaoMapSdkPromise = null;
      console.error("[Honeytong Map Debug] Kakao map SDK script failed", error);
      reject(error);
    };

    const handleLoad = () => {
      if (!window.kakao?.maps) {
        handleError(
          new Error("Kakao maps object is not available after script load"),
        );
        return;
      }

      window.kakao.maps.load(() => {
        kakaoMapSdkReady = true;
        resolve(window.kakao!);
      });
    };

    const existingScript = document.querySelector<HTMLScriptElement>(
      `#${KAKAO_MAP_SCRIPT_ID}, script[data-kakao-map-sdk="true"]`,
    );
    if (existingScript) {
      existingScript.addEventListener("load", handleLoad, { once: true });
      existingScript.addEventListener(
        "error",
        () => handleError(new Error("Kakao map SDK script failed to load")),
        { once: true },
      );
      return;
    }

    const script = document.createElement("script");
    script.id = KAKAO_MAP_SCRIPT_ID;
    script.dataset.kakaoMapSdk = "true";
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(
      appKey,
    )}&autoload=false`;
    script.onload = handleLoad;
    script.onerror = () => {
      handleError(new Error("Kakao map SDK script failed to load"));
    };
    document.head.appendChild(script);
  });

  return kakaoMapSdkPromise;
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
    CustomOverlay: new (options: {
      map?: KakaoMap;
      position: KakaoLatLng;
      content: string | HTMLElement;
      yAnchor?: number;
      xAnchor?: number;
      zIndex?: number;
    }) => KakaoOverlay;
    event: {
      addListener: (
        target: KakaoMarker | KakaoMap,
        type: "click" | "dragstart" | "dragend" | "center_changed",
        handler: () => void,
      ) => void;
    };
  };
};

export type KakaoLatLng = {
  toString: () => string;
};

export type KakaoLatLngBounds = {
  extend: (latLng: KakaoLatLng) => void;
};

export type KakaoMap = {
  getCenter: () => KakaoLatLng;
  getLevel: () => number;
  getProjection: () => KakaoMapProjection;
  relayout: () => void;
  setDraggable: (draggable: boolean) => void;
  setBounds: (bounds: KakaoLatLngBounds) => void;
  setCenter: (latLng: KakaoLatLng) => void;
  setLevel: (
    level: number,
    options?: { animate?: boolean | { duration: number }; anchor?: KakaoLatLng },
  ) => void;
  setZoomable: (zoomable: boolean) => void;
};

export type KakaoMapProjection = {
  containerPointFromCoords: (latLng: KakaoLatLng) => { x: number; y: number };
};

export type KakaoMarker = {
  setMap: (map: KakaoMap | null) => void;
};

export type KakaoOverlay = {
  setMap: (map: KakaoMap | null) => void;
};

declare global {
  interface Window {
    kakao?: KakaoWindow;
  }
}
