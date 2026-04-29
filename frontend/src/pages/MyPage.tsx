import BottomNav from "../components/BottomNav";

export default function MyPage() {
  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <h1 className="text-2xl font-bold">마이페이지</h1>

        <section className="mt-6 rounded-2xl bg-white p-5 text-left shadow-sm">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-yellow-300 text-2xl">
              꿀
            </div>

            <div>
              <h2 className="text-lg font-bold">허니 탐험가</h2>
              <p className="text-sm text-gray-500">
                오늘도 맛있는 하루 보내세요.
              </p>
            </div>
          </div>
        </section>

        <section className="mt-5 rounded-2xl bg-white p-5 text-left shadow-sm">
          <h2 className="text-lg font-bold">나의 활동</h2>

          <div className="mt-4 flex justify-between text-center">
            <div>
              <p className="text-xl font-bold">3</p>
              <p className="text-xs text-gray-500">찜</p>
            </div>
            <div>
              <p className="text-xl font-bold">5</p>
              <p className="text-xs text-gray-500">방문</p>
            </div>
            <div>
              <p className="text-xl font-bold">2</p>
              <p className="text-xs text-gray-500">리뷰</p>
            </div>
          </div>
        </section>

        <section className="mt-5 rounded-2xl bg-white p-5 text-left shadow-sm">
          <h2 className="text-lg font-bold">설정</h2>

          <div className="mt-4 flex flex-col gap-4 text-sm">
            <button type="button" className="text-left">
              프로필 수정
            </button>
            <button type="button" className="text-left">
              알림 설정
            </button>
            <button type="button" className="text-left">
              고객센터
            </button>
          </div>
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
