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
            getApiErrorMessage(error, "동네 이야기를 불러오지 못했어요."),
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
        setMessage("게시글이 수정되었어요.");
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
          editing
            ? "게시글 수정을 처리하지 못했어요."
            : "게시글 작성을 처리하지 못했어요.",
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
      return "첫 이야기를 기다리고 있어요";
    }
    return `동네 이야기 ${posts.length}개`;
  }, [posts.length]);

  return (
    <div className="min-h-screen bg-m3-surface lg:bg-m3-surface-container">
      <main className="mx-auto min-h-screen w-full max-w-[430px] bg-m3-surface px-4 pb-[calc(96px+env(safe-area-inset-bottom))] pt-6 text-m3-on-surface sm:px-5 lg:grid lg:max-w-none lg:grid-cols-[minmax(360px,440px)_minmax(0,720px)] lg:gap-6 lg:bg-transparent lg:px-8 lg:pb-10 lg:pt-8 xl:max-w-[1220px]">
        <section className="lg:sticky lg:top-8 lg:flex lg:max-h-[calc(100dvh-64px)] lg:min-h-0 lg:flex-col">
          <header className="rounded-m3-xl bg-m3-surface-container-low p-5 shadow-m3-1">
            <p className="text-m3-label-md text-m3-primary">커뮤니티</p>
            <h1 className="mt-1 text-[28px] font-semibold leading-9 tracking-normal text-m3-on-surface lg:text-[32px] lg:leading-10">
              동네 자유게시판
            </h1>
            <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
              가볍게 나누고 싶은 동네 소식을 함께 올려보세요.
            </p>
          </header>

          {message && (
            <section className="mt-5 rounded-m3-lg border border-m3-outline-variant bg-m3-secondary-container px-4 py-3 text-m3-body-md text-m3-on-secondary-container shadow-m3-1">
              {message}
            </section>
          )}

          <section className="mt-5 rounded-m3-xl bg-m3-surface-container-low p-4 shadow-m3-1 lg:min-h-0 lg:flex-1 lg:overflow-y-auto lg:p-5">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-m3-label-md text-m3-primary">
                  {editing ? "게시글 수정" : "새 글 작성"}
                </p>
                <h2 className="mt-1 text-m3-title-lg text-m3-on-surface">
                  {editing ? "내용을 다듬고 저장해요" : "오늘의 이야기를 남겨요"}
                </h2>
              </div>
              {editing && (
                <button
                  type="button"
                  onClick={cancelEdit}
                  className="h-9 shrink-0 rounded-m3-full bg-m3-surface-container-high px-3 text-m3-label-md text-m3-on-surface-variant transition hover:bg-m3-surface-container-highest"
                >
                  취소
                </button>
              )}
            </div>

            {!canWrite && !editing && (
              <p className="mt-3 rounded-m3-lg bg-m3-primary-container px-3 py-3 text-m3-body-sm text-m3-on-primary-container">
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
                className="h-14 w-full rounded-m3-sm border border-m3-outline bg-m3-surface-container-lowest px-4 text-m3-body-lg text-m3-on-surface outline-none transition placeholder:text-m3-on-surface-variant focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20"
              />
              <textarea
                value={form.content}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, content: event.target.value }))
                }
                placeholder="동네 사람들과 나누고 싶은 이야기를 적어주세요."
                maxLength={2000}
                rows={6}
                className="min-h-40 w-full resize-none rounded-m3-sm border border-m3-outline bg-m3-surface-container-lowest px-4 py-3 text-m3-body-lg text-m3-on-surface outline-none transition placeholder:text-m3-on-surface-variant focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20 lg:min-h-44"
              />
              <button
                type="button"
                onClick={submitPost}
                disabled={submitDisabled}
                className="h-12 rounded-m3-full bg-m3-primary px-4 text-m3-label-lg text-m3-on-primary shadow-m3-1 transition hover:brightness-105 active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-m3-surface-container-highest disabled:text-m3-on-surface-variant disabled:shadow-none"
              >
                {busyAction === "submit"
                  ? "저장 중"
                  : editing
                    ? "수정 완료"
                    : "게시하기"}
              </button>
            </div>
          </section>
        </section>

        <section className="mt-5 flex min-w-0 flex-col gap-5 lg:mt-0 lg:min-h-0 lg:grid lg:grid-cols-[minmax(0,1fr)_minmax(300px,360px)]">
          <section className="min-w-0 rounded-m3-xl bg-m3-surface-container-low p-4 shadow-m3-1 lg:min-h-0 lg:overflow-hidden lg:p-5">
            <div className="flex items-center justify-between gap-3">
              <p className="text-m3-title-sm text-m3-on-surface">{listTitle}</p>
              <span className="rounded-m3-full bg-m3-secondary-container px-3 py-1 text-m3-label-md text-m3-on-secondary-container">
                최신순
              </span>
            </div>

            <div className="lg:desktop-compact-scrollbar lg:max-h-[calc(100dvh-146px)] lg:overflow-y-auto lg:pr-1">
              {busyAction === "load" && (
                <StateCard title="동네 이야기를 불러오는 중이에요." />
              )}

              {busyAction !== "load" && posts.length === 0 && (
                <StateCard
                  title="아직 게시글이 없어요"
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
                      className={`w-full rounded-m3-lg border p-4 text-left transition ${
                        selectedPost?.postId === post.postId
                          ? "border-m3-primary bg-m3-primary-container text-m3-on-primary-container"
                          : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface hover:border-m3-primary hover:bg-m3-surface-container"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <p className="truncate text-m3-title-sm">
                            {post.title}
                          </p>
                          <p className="mt-1 truncate text-m3-body-sm text-m3-on-surface-variant">
                            {post.authorNickname} · {formatDate(post.createdAt)}
                          </p>
                        </div>
                        {post.mine && (
                          <span className="shrink-0 rounded-m3-full bg-m3-primary px-2 py-1 text-[10px] font-semibold leading-4 text-m3-on-primary">
                            내 글
                          </span>
                        )}
                      </div>
                      <p className="mt-3 line-clamp-2 text-m3-body-md text-m3-on-surface-variant">
                        {post.content}
                      </p>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </section>

          <section className="min-w-0 rounded-m3-xl bg-m3-surface-container-low p-5 shadow-m3-1 lg:min-h-0 lg:self-start">
            {selectedPost ? (
              <>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate text-m3-label-md text-m3-primary">
                      {selectedPost.authorNickname} ·{" "}
                      {formatDate(selectedPost.createdAt)}
                    </p>
                    <h2 className="mt-2 break-words text-m3-title-lg text-m3-on-surface">
                      {selectedPost.title}
                    </h2>
                  </div>
                  <button
                    type="button"
                    onClick={() => setSelectedPost(null)}
                    className="h-9 shrink-0 rounded-m3-full bg-m3-surface-container-high px-3 text-m3-label-md text-m3-on-surface-variant transition hover:bg-m3-surface-container-highest"
                  >
                    닫기
                  </button>
                </div>
                <p className="mt-4 whitespace-pre-wrap break-words text-m3-body-md text-m3-on-surface-variant lg:max-h-[calc(100dvh-260px)] lg:overflow-y-auto">
                  {selectedPost.content}
                </p>

                {selectedPost.mine && (
                  <div className="mt-5 grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={startEdit}
                      className="h-12 rounded-m3-full bg-m3-primary px-4 text-m3-label-lg text-m3-on-primary shadow-m3-1 transition hover:brightness-105"
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      onClick={removePost}
                      disabled={busyAction === "delete"}
                      className="h-12 rounded-m3-full bg-m3-surface-container-high px-4 text-m3-label-lg text-m3-on-surface transition hover:bg-m3-surface-container-highest disabled:cursor-not-allowed disabled:text-m3-on-surface-variant"
                    >
                      {busyAction === "delete" ? "삭제 중" : "삭제"}
                    </button>
                  </div>
                )}
              </>
            ) : (
              <StateCard
                title="읽을 글을 선택해 주세요"
                desc="데스크톱에서는 선택한 글이 이 영역에 바로 열려요."
              />
            )}
          </section>
        </section>
      </main>
      <div className="lg:hidden">
        <BottomNav />
      </div>
    </div>
  );
}

function StateCard({ title, desc }: { title: string; desc?: string }) {
  return (
    <div className="mt-4 rounded-m3-lg bg-m3-surface-container p-4 text-center">
      <p className="text-m3-title-sm text-m3-on-surface">{title}</p>
      {desc && (
        <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
          {desc}
        </p>
      )}
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
