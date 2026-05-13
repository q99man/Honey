import { useEffect, useMemo, useState } from "react";
import {
  createCommunityPost,
  deleteCommunityPost,
  getCommunityPost,
  getCommunityPosts,
  updateCommunityPost,
  type CommunityPost,
} from "../api/communityApi";
import { getApiErrorMessage, hasStoredAccessToken } from "../api/http";
import BottomNav from "../components/BottomNav";

type BusyAction = "load" | "submit" | "delete" | null;

const EMPTY_FORM = {
  title: "",
  content: "",
};

export default function CommunityPage() {
  const [posts, setPosts] = useState<CommunityPost[]>([]);
  const [selectedPost, setSelectedPost] = useState<CommunityPost | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editing, setEditing] = useState(false);
  const [busyAction, setBusyAction] = useState<BusyAction>("load");
  const [message, setMessage] = useState<string | null>(null);

  const canWrite = hasStoredAccessToken();
  const trimmedTitle = form.title.trim();
  const trimmedContent = form.content.trim();
  const submitDisabled =
    busyAction === "submit" ||
    trimmedTitle.length === 0 ||
    trimmedContent.length === 0;

  useEffect(() => {
    let mounted = true;
    getCommunityPosts()
      .then((nextPosts) => {
        if (mounted) {
          setPosts(nextPosts);
        }
      })
      .catch((error) => {
        if (mounted) {
          setMessage(
            getApiErrorMessage(
              error,
              "동네 이야기를 불러오지 못했어요.",
            ),
          );
        }
      })
      .finally(() => {
        if (mounted) {
          setBusyAction(null);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const openPost = async (postId: number) => {
    setMessage(null);
    try {
      const post = await getCommunityPost(postId);
      setSelectedPost(post);
      setEditing(false);
      setForm(EMPTY_FORM);
    } catch (error) {
      setMessage(getApiErrorMessage(error, "게시글을 불러오지 못했어요."));
    }
  };

  const startEdit = () => {
    if (!selectedPost) {
      return;
    }
    setForm({
      title: selectedPost.title,
      content: selectedPost.content,
    });
    setEditing(true);
    setMessage(null);
  };

  const cancelEdit = () => {
    setForm(EMPTY_FORM);
    setEditing(false);
    setMessage(null);
  };

  const submitPost = async () => {
    setBusyAction("submit");
    setMessage(null);
    try {
      if (editing && selectedPost) {
        const updated = await updateCommunityPost(selectedPost.postId, {
          title: trimmedTitle,
          content: trimmedContent,
        });
        setSelectedPost(updated);
        setPosts((prev) =>
          prev.map((post) =>
            post.postId === updated.postId ? updated : post,
          ),
        );
        setMessage("게시글을 수정했어요.");
      } else {
        const created = await createCommunityPost({
          title: trimmedTitle,
          content: trimmedContent,
        });
        const post = await getCommunityPost(created.postId);
        setPosts((prev) => [post, ...prev]);
        setSelectedPost(post);
        setMessage("게시글을 올렸어요.");
      }
      setForm(EMPTY_FORM);
      setEditing(false);
    } catch (error) {
      setMessage(
        getApiErrorMessage(
          error,
          editing ? "게시글 수정을 처리하지 못했어요." : "게시글 작성을 처리하지 못했어요.",
        ),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const removePost = async () => {
    if (!selectedPost) {
      return;
    }
    setBusyAction("delete");
    setMessage(null);
    try {
      await deleteCommunityPost(selectedPost.postId);
      setPosts((prev) =>
        prev.filter((post) => post.postId !== selectedPost.postId),
      );
      setSelectedPost(null);
      setEditing(false);
      setForm(EMPTY_FORM);
      setMessage("게시글을 삭제했어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "게시글 삭제를 처리하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  const listTitle = useMemo(() => {
    if (posts.length === 0) {
      return "첫 이야기를 기다리고 있어요.";
    }
    return `동네 이야기 ${posts.length}개`;
  }, [posts.length]);

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] px-4 pb-24 pt-6">
        <header>
          <p className="text-xs font-semibold text-[#d99a00]">커뮤니티</p>
          <h1 className="mt-1 text-2xl font-bold text-[#2b210f]">
            동네 자유게시판
          </h1>
          <p className="mt-2 text-sm leading-6 text-gray-600">
            가볍게 나누고 싶은 동네 소식을 남겨보세요.
          </p>
        </header>

        {message && (
          <section className="mt-5 rounded-2xl border border-[#f6d365] bg-white px-4 py-3 text-sm font-semibold leading-6 text-[#8a6315] shadow-sm">
            {message}
          </section>
        )}

        <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-bold text-[#d99a00]">
                {editing ? "게시글 수정" : "새 글 작성"}
              </p>
              <h2 className="mt-1 text-lg font-black text-[#2b210f]">
                {editing ? "내용을 다듬고 저장해요" : "오늘의 이야기를 남겨요"}
              </h2>
            </div>
            {editing && (
              <button
                type="button"
                onClick={cancelEdit}
                className="shrink-0 rounded-full bg-gray-100 px-3 py-2 text-xs font-bold text-gray-600"
              >
                취소
              </button>
            )}
          </div>

          {!canWrite && !editing && (
            <p className="mt-3 rounded-2xl bg-[#fff7d6] px-3 py-3 text-xs font-bold leading-5 text-[#8a6315]">
              글 작성은 로그인 후 이용할 수 있어요.
            </p>
          )}

          <div className="mt-4 flex flex-col gap-3">
            <input
              value={form.title}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, title: event.target.value }))
              }
              placeholder="제목"
              maxLength={120}
              className="w-full rounded-2xl border border-gray-100 bg-[#fafafa] px-4 py-3 text-sm font-bold text-[#2b210f] outline-none placeholder:text-gray-400 focus:border-[#f6b800] focus:bg-white"
            />
            <textarea
              value={form.content}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, content: event.target.value }))
              }
              placeholder="동네 사람들과 나누고 싶은 이야기를 적어주세요."
              maxLength={2000}
              rows={5}
              className="w-full resize-none rounded-2xl border border-gray-100 bg-[#fafafa] px-4 py-3 text-sm font-semibold leading-6 text-[#2b210f] outline-none placeholder:text-gray-400 focus:border-[#f6b800] focus:bg-white"
            />
            <button
              type="button"
              onClick={submitPost}
              disabled={submitDisabled}
              className="rounded-2xl bg-[#2b210f] px-4 py-3 text-sm font-black text-white transition disabled:cursor-not-allowed disabled:bg-gray-300"
            >
              {busyAction === "submit"
                ? "저장 중"
                : editing
                  ? "수정 완료"
                  : "게시하기"}
            </button>
          </div>
        </section>

        <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
          <p className="text-sm font-black text-[#2b210f]">{listTitle}</p>

          {busyAction === "load" && (
            <StateCard title="동네 이야기를 불러오는 중이에요." />
          )}

          {busyAction !== "load" && posts.length === 0 && (
            <StateCard
              title="아직 게시글이 없어요."
              desc="첫 소식을 남기면 이곳에 표시돼요."
            />
          )}

          {posts.length > 0 && (
            <div className="mt-4 flex flex-col gap-3">
              {posts.map((post) => (
                <button
                  key={post.postId}
                  type="button"
                  onClick={() => openPost(post.postId)}
                  className={`w-full rounded-2xl border p-4 text-left transition ${
                    selectedPost?.postId === post.postId
                      ? "border-[#f6b800] bg-[#fff8de]"
                      : "border-gray-100 bg-[#fafafa] hover:border-[#f6d365]"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-black text-[#2b210f]">
                        {post.title}
                      </p>
                      <p className="mt-1 text-xs font-semibold text-gray-500">
                        {post.authorNickname} · {formatDate(post.createdAt)}
                      </p>
                    </div>
                    {post.mine && (
                      <span className="shrink-0 rounded-full bg-[#f6b800] px-2 py-1 text-[10px] font-black text-white">
                        내 글
                      </span>
                    )}
                  </div>
                  <p className="mt-3 line-clamp-2 text-sm leading-6 text-gray-600">
                    {post.content}
                  </p>
                </button>
              ))}
            </div>
          )}
        </section>

        {selectedPost && (
          <section className="mt-5 rounded-3xl bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-xs font-bold text-[#d99a00]">
                  {selectedPost.authorNickname} ·{" "}
                  {formatDate(selectedPost.createdAt)}
                </p>
                <h2 className="mt-2 break-words text-xl font-black leading-7 text-[#2b210f]">
                  {selectedPost.title}
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setSelectedPost(null)}
                className="shrink-0 rounded-full bg-gray-100 px-3 py-2 text-xs font-bold text-gray-600"
              >
                닫기
              </button>
            </div>
            <p className="mt-4 whitespace-pre-wrap break-words text-sm font-semibold leading-7 text-gray-700">
              {selectedPost.content}
            </p>

            {selectedPost.mine && (
              <div className="mt-5 grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={startEdit}
                  className="rounded-2xl bg-[#f6b800] px-4 py-3 text-sm font-black text-white"
                >
                  수정
                </button>
                <button
                  type="button"
                  onClick={removePost}
                  disabled={busyAction === "delete"}
                  className="rounded-2xl bg-gray-100 px-4 py-3 text-sm font-black text-gray-700 disabled:cursor-not-allowed disabled:text-gray-400"
                >
                  {busyAction === "delete" ? "삭제 중" : "삭제"}
                </button>
              </div>
            )}
          </section>
        )}
      </main>
      <BottomNav />
    </div>
  );
}

function StateCard({ title, desc }: { title: string; desc?: string }) {
  return (
    <div className="mt-4 rounded-2xl bg-[#fafafa] p-4 text-center">
      <p className="text-sm font-bold text-[#2b210f]">{title}</p>
      {desc && <p className="mt-2 text-sm leading-6 text-gray-500">{desc}</p>}
    </div>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "방금";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}
