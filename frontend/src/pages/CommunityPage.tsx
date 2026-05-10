import BottomNav from "../components/BottomNav";

export default function CommunityPage() {
  return (
    <div className="min-h-screen bg-[#fffbeb] pb-24">
      <main className="mx-auto min-h-screen max-w-[430px] px-5 pt-6">
        <section className="rounded-[24px] border border-[#f6d365] bg-white p-5 shadow-sm">
          <p className="text-xs font-bold text-[#d99a00]">커뮤니티</p>
          <h1 className="mt-2 text-xl font-black text-[#2b210f]">
            동네 이야기를 준비하고 있어요.
          </h1>
          <p className="mt-3 text-sm leading-6 text-gray-600">
            지금은 지도 탐색, 랭킹, 저장, 마이페이지 기능을 먼저 사용할 수 있어요.
          </p>
        </section>
      </main>
      <BottomNav />
    </div>
  );
}
