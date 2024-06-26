const board = {
    init: function() {
        this.bindEvents();
        this.selectedFiles = []; // 선택된 파일을 저장할 배열
        this.deletedFiles = [];  // 삭제할 파일 번호를 저장할 배열
    },

    bindEvents: function() {
        document.getElementById('btn-modify-board')?.addEventListener('click', () => {
            const boardNo = document.querySelector(".board-no").value;
            window.location.href = `/crispy/freeBoardModify/${boardNo}`;
        });
        document.querySelector(".board-list")?.addEventListener("click", () => {
            location.href = "/crispy/board-list/free";
        })
        document.getElementById("btn-cancel")?.addEventListener("click", function () {
            const boardNo = document.querySelector(".board-no").value;
            location.href= `/crispy/freeBoardDetail/${boardNo}`;
        })

        const addBoard = document.getElementById("addBtn");
        addBoard?.addEventListener("click", this.addBoard.bind(this));

        // 파일 인풋 요소에 change 이벤트 리스너 추가
        const fileInput = document.getElementById("formFileMultiple");
        fileInput?.addEventListener("change", this.handleFileSelect.bind(this));

        // 기타 이벤트 바인딩
        const modifyBoard = document.querySelector(".btn-modify");
        modifyBoard?.addEventListener("click", this.modifyBoard.bind(this));

        document.querySelector('.board-delete')?.addEventListener("click", this.deleteBoard.bind(this));
        document.getElementById("comment-add")?.addEventListener("click", this.createComment.bind(this));
        document.getElementById("like-button")?.addEventListener("click", this.toggleLike.bind(this));

        this.bindCommentEvents();
        this.bindFileDownloadEvents();
        this.bindExistingFileDeleteEvents();
        this.handleLine()
    },

    handleLine: function () {
        const boardContentElement = document.querySelector(".board-content");
        let boardContent = boardContentElement.textContent || boardContentElement.innerText;

        boardContent = boardContent.replace(/\n/g, "<br>");

        boardContent = boardContent.replace(/^(<br\s*\/?>)+/, '');

        boardContentElement.innerHTML = boardContent;

        const comments = document.querySelectorAll('.cmt-content');
        comments.forEach(comment => {
            const content = comment.textContent || comment.innerText;
            comment.innerHTML = content.replace(/\n/g, "<br>");
        });

    },

    handleFileSelect: function(event) {
        const files = event.target.files;
        this.selectedFiles = Array.from(files); // 파일 배열 초기화 후 선택된 파일들 추가
        this.updateFileList();
        this.updateFileInput();
    },

    updateFileList: function() {
        const fileList = document.querySelector('.file-list');
        fileList.innerHTML = '';

        this.selectedFiles.forEach((file, index) => {
            const fileItem = document.createElement('div');
            fileItem.classList.add('file-item');
            fileItem.innerHTML = `
                <span>${file.name}</span>
                <a class="delete-file" data-index="${index}" >
                <i class="fa-regular fa-circle-xmark" style="font-size: 1.7rem"></i>
                </a>
            `;
            fileList.appendChild(fileItem);
        });

        // 삭제 버튼 이벤트 리스너 추가
        const deleteButtons = document.querySelectorAll('.delete-file');
        deleteButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const index = e.target.getAttribute('data-index');
                this.selectedFiles.splice(index, 1);
                this.updateFileList();
                this.updateFileInput();
            });
        });
    },

    updateFileInput: function() {
        const fileInput = document.getElementById('formFileMultiple');
        const dataTransfer = new DataTransfer();

        this.selectedFiles.forEach(file => {
            dataTransfer.items.add(file);
        });

        fileInput.files = dataTransfer.files;
    },

    bindExistingFileDeleteEvents: function() {
        const deleteButtons = document.querySelectorAll('.existing-file-delete');
        console.log(deleteButtons);
        deleteButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const fileId = button.getAttribute('data-file-id');
                this.deletedFiles.push(fileId);
                button.closest('li').remove();
            });
        });
    },

    // 게시판 추가
    addBoard: function() {
        const form = document.getElementById('frm-board-add');
        const formData = new FormData(form);

        let summernoteContent = $('#summernote').summernote('code');

        summernoteContent = summernoteContent.replace(/<br\s*\/?>/gi, "\n");

        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = summernoteContent;
        const boardContent = tempDiv.innerHTML.replace(/<br\s*\/?>/gi, "\n");

        const data = {

            boardTitle: form.boardTitle.value,
            boardContent: boardContent
        };

        formData.append('freeBoardDto', new Blob([JSON.stringify(data)], { type: 'application/json' }));

        fetch('/api/freeBoard/v1', {
            method: "POST",
            body: formData
        }).then(response => {
            if (!response.ok) {
                return response.json().then(err => {
                    this.displayValidationErrors(err);
                });
            } else {
                return response.json();
            }
        }).then(data => {
            Swal.fire({
                icon: "success",
                title: data.message,
                showConfirmButton: false,
                timer: 1500
            }).then(() => {
                const boardNo = data.boardNo;
                location.href = `/crispy/freeBoardDetail/${boardNo}`;
            })
        }).catch(error => {
        });
    },

    // 검증 메소드
    displayValidationErrors: function(errors) {
        Object.keys(errors).forEach(field => {
            const errorContainer = document.querySelector(`.${field}-error`);
            if (errorContainer) {
                errorContainer.textContent = errors[field];
                errorContainer.style.display = 'block';
            }
        });
    },

    // 게시판 수정
    modifyBoard: function() {
        const form = document.getElementById('frm-board-modify');
        const formData = new FormData(form);

        let summernoteContent = $('#summernote').summernote('code');

        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = summernoteContent;
        const boardContent = tempDiv.innerText || tempDiv.textContent || "";

        const data = {
            boardNo: parseInt(document.querySelector(".board-no").value),
            boardTitle: document.querySelector(".board-title").value,
            boardContent: summernoteContent
        };

        formData.append('freeBoardDto', new Blob([JSON.stringify(data)], { type: 'application/json' }));


        formData.append('deletedFileNo', new Blob([JSON.stringify(this.deletedFiles)], { type: 'application/json' }));

        fetch('/api/freeBoard/v1', {
            method: "PUT",
            body: formData
        }).then(response => {
            if (!response.ok) {
                return response.json().then(err => {
                    this.displayValidationErrors(err);
                });
            } else {
                return response.json();
            }
        }).then(data => {
            Swal.fire({
                icon: "success",
                title: data.message,
                showConfirmButton: false,
                timer: 1500
            }).then(() => {
                const boardNo = data.boardNo;
                location.href = `/crispy/freeBoardDetail/${boardNo}`;
            })
        }).catch(error => {
        });
    },


    // 게시판 삭제
    deleteBoard: function() {
        const boardNo = document.querySelector(".board-no").value;
        const empNo = document.querySelector(".emp-no")?.value;

        Swal.fire({
            title: '정말로 이 게시글을 삭제하시겠습니까?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: '네, 삭제하겠습니다!',
            cancelButtonText: '아니요, 취소합니다'
        }).then((result) => {
            if (result.isConfirmed) {
                fetch(`/api/freeBoard/${boardNo}/v1`, {
                    method: "DELETE",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ boardNo: boardNo, empNo: empNo })
                }).then(response => response.json())
                    .then(data => {
                        Swal.fire({
                            icon: "success",
                            title: data.message,
                            showConfirmButton: false,
                            timer: 1500
                        }).then(() => {
                            location.href = "/crispy/board-list/free";
                        });
                    }).catch(error => {
                    Swal.fire({
                        icon: "error",
                        title: "삭제에 실패했습니다.",
                        showConfirmButton: false,
                        timer: 1500
                    });
                });
            }
        });
    },


    // 좋아요 토글
    toggleLike: function() {
        const boardNo = document.querySelector(".board-no").value;
        const likeButton = document.getElementById("like-button");
        const likeCountElement = document.getElementById("like-count");

        fetch(`/api/freeBoard/${boardNo}/like/v1`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ boardNo: parseInt(boardNo) })
        }).then(response => response.json())
            .then(data => {
                if (data.message) {
                    const isLiked = likeButton.getAttribute("src") === '/img/heart2.png';
                    if (isLiked) {
                        likeButton.setAttribute("src", "/img/heart1.png");
                        likeCountElement.textContent = (parseInt(likeCountElement.textContent) - 1).toString();
                    } else {
                        likeButton.setAttribute("src", "/img/heart2.png");
                        likeCountElement.textContent =(parseInt(likeCountElement.textContent) + 1).toString();
                    }
                } else {
                    throw new Error("Failed to toggle like");
                }
            })
            .catch(error => {
                Swal.fire({
                    icon: "error",
                    title: "Error",
                    text: "좋아요 상태 변경에 실패했습니다."
                });
            });
    },

    // 댓글 생성
    createComment: function() {
        const boardNo = document.querySelector(".board-no").value;
        const cmtContent = document.getElementById("cmt-comment").value;
        const parentCmtNo = null;
        const data = {
            boardNo: parseInt(boardNo),
            cmtContent: cmtContent,
            parentCmtNo: parentCmtNo
        }

        fetch("/api/comments/v1", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        }).then(response => {
            if (!response.ok) {
                return response.json().then(err => {
                    this.displayValidationErrors(err);
                });
            } else {
                return response.json()
            }
        }).then(data => {
            Swal.fire({
                icon: "success",
                title: data.message,
                showConfirmButton: false,
                timer: 1500
            }).then(() => {
                location.reload()
            })
        }).catch(error => {
            console.error('Error:', error);
        });
    },

    bindCommentEvents: function() {
        const cmtModify = document.querySelectorAll('.comment-modify');
        cmtModify.forEach(button => {
            button.addEventListener("click", function() {
                const commentDiv = this.closest(".comment-level-1, .comment-level-2"); // 댓글 블록 선택
                if (commentDiv) {
                    // 기존 HTML 구조 저장
                    const originalHTML = commentDiv.innerHTML;

                    const originalContent = commentDiv.querySelector(".cmt-content").innerHTML.replace(/<br\s*\/?>/gi, '\n');
                    const profile = commentDiv.querySelector(".comment-profile") ? commentDiv.querySelector(".comment-profile").outerHTML : '';
                    const name = commentDiv.querySelector("span") ? commentDiv.querySelector("span").outerHTML : '';
                    const date = commentDiv.querySelector(".date") ? commentDiv.querySelector(".date").outerHTML : '';

                    const cmtNo = this.getAttribute("data-comment-no");

                    const cmtModifyWrapper = document.createElement("div");
                    cmtModifyWrapper.className = "cmt-modify-wrapper";

                    const textarea = document.createElement("textarea");
                    textarea.classList.add("form-control");
                    textarea.value = originalContent;

                    const saveButton = document.createElement("button");
                    saveButton.type = "button";
                    saveButton.classList.add("btn", "btn-primary");
                    saveButton.innerText = "저장";
                    saveButton.addEventListener("click", function() {
                        const updatedContent = textarea.value.replace(/\n/g, '<br>');
                        const data = {
                            boardNo: parseInt(document.querySelector(".board-no").value),
                            cmtNo: parseInt(cmtNo),
                            cmtContent: updatedContent
                        };
                        fetch(`/api/comments/v1`, {
                            method: "PUT",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(data)
                        }).then(response => {
                            if (!response.ok) {
                                return response.json().then(err => {
                                    alert("댓글 수정에 실패했습니다.");
                                });
                            } else {
                                return response.json();
                            }
                        }).then(data => {
                            Swal.fire({
                                icon: "success",
                                title: data.message,
                                showConfirmButton: false,
                                timer: 1500
                            }).then(() => {
                                location.reload();
                            })
                        }).catch(error => {
                        });
                    });

                    const cancelButton = document.createElement("button");
                    cancelButton.type = "button";
                    cancelButton.classList.add("btn", "btn-cmt-cancel");
                    cancelButton.innerText = "취소";
                    cancelButton.addEventListener("click", function() {
                        commentDiv.innerHTML = originalHTML;
                        board.bindCommentEvents(); // 이벤트 다시 바인딩
                    });

                    cmtModifyWrapper.appendChild(textarea);
                    cmtModifyWrapper.appendChild(saveButton);
                    cmtModifyWrapper.appendChild(cancelButton);

                    commentDiv.innerHTML = '';
                    commentDiv.appendChild(cmtModifyWrapper);
                }
            });
        });

        const cmtDelete = document.querySelectorAll('.comment-delete');
        cmtDelete.forEach(button => {
            button.addEventListener("click", function() {
                const cmtNo = this.getAttribute("data-comment-no");

                // SweetAlert Confirm
                Swal.fire({
                    title: '댓글을 삭제하시겠습니까?',
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#3085d6',
                    cancelButtonColor: '#d33',
                    confirmButtonText: '네, 삭제하겠습니다',
                    cancelButtonText: '취소'
                }).then((result) => {
                    if (result.isConfirmed) {
                        fetch(`/api/comments/${cmtNo}/v1`, {
                            method: "DELETE",
                            headers: { "Content-Type": "application/json" }
                        }).then(response => response.json())
                            .then(data => {
                                Swal.fire({
                                    icon: 'success',
                                    title: data.message,
                                    showConfirmButton: false,
                                    timer: 1500
                                }).then(() => {
                                    location.reload();
                                });
                            }).catch(error => {
                            Swal.fire({
                                icon: 'error',
                                title: '댓글 삭제에 실패했습니다.',
                                showConfirmButton: false,
                                timer: 1500
                            });
                        });
                    }
                });
            });
        });

        const replyButton = document.querySelectorAll('#reply-button');
        replyButton.forEach(button => {
            button.addEventListener("click", function() {
                const replyContainer = this.closest('.reply-container');
                if (!replyContainer.querySelector('.reply-input')) {
                    const replyWrapper = document.createElement('div');
                    replyWrapper.classList.add('reply-wrapper');

                    const input = document.createElement('textarea');
                    input.classList.add('form-control', 'reply-input');
                    input.placeholder = '답글을 입력하세요';
                    input.rows = 2;

                    const submitButton = document.createElement('button');
                    submitButton.classList.add('btn', 'btn-primary', 'reply-submit');
                    submitButton.textContent = '등록';
                    submitButton.addEventListener("click", function() {
                        const parentCmtNo = replyContainer.querySelector('.cmt-no').value;
                        const cmtContent = input.value;
                        const data = {
                            boardNo: parseInt(document.querySelector(".board-no").value),
                            cmtContent: cmtContent,
                            parentCmtNo: parseInt(parentCmtNo)
                        }
                        fetch("/api/comments/v1", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify(data)
                        }).then(response => {
                            if (!response.ok) {
                                return response.json().then(err => {
                                    const errorMessage = err.cmtContent || "답글 등록 실패";
                                    Swal.fire({
                                        icon: 'error',
                                        title: 'Error',
                                        text: errorMessage
                                    });
                                });
                            } else {
                                return response.json();
                            }
                        }).then(data => {
                            Swal.fire({
                                icon: "success",
                                title: data.message,
                                showConfirmButton: false,
                                timer: 1500
                            }).then(() => {
                                location.reload();
                            })
                        }).catch(error => {
                        });
                    });

                    replyWrapper.appendChild(input);
                    replyWrapper.appendChild(submitButton);
                    replyContainer.appendChild(replyWrapper);
                }
            });
        });
    },

    bindFileDownloadEvents: function() {
        // 단일 파일 다운로드
        const fileDownload = document.querySelectorAll(".file-download");
        fileDownload?.forEach(function(element) {
            element.addEventListener("click", function() {
                const boardFileNo = this.dataset.fileNo;
                window.location.href = `/api/freeBoard/file/download?boardFileNo=${boardFileNo}`;
            });
        });

        // 전체 파일 다운로드
        const downloadAll = document.getElementById("downloadAllBtn");
        downloadAll?.addEventListener("click", function() {
            const boardNo = document.querySelector(".board-no").value;
            window.location.href = `/api/freeBoard/file/downloadAll?boardNo=${boardNo}`;
        });
    }
};

document.addEventListener("DOMContentLoaded", function() {
    board.init();
});
