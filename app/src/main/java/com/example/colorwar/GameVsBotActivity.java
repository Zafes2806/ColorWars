package com.example.colorwar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Random;

public class GameVsBotActivity extends AppCompatActivity implements CellAdapter.OnCellClickListener {

    private static final String TAG = "GameVsBotActivity";
    private RecyclerView gameGrid;
    private ConstraintLayout mainLayout;
    private CellAdapter cellAdapter;
    private int[] cellStates; // 0: trống, 1-3: đỏ (1-3 chấm), 4-6: xanh (1-3 chấm), 7: đỏ (4 chấm, tạm thời), 8: xanh (4 chấm, tạm thời)
    private boolean isPlayer1Turn = true; // true: Người chơi (đỏ), false: Bot (xanh)
    private boolean player1FirstClick = true; // Theo dõi lần nhấn đầu tiên của Người chơi
    private boolean botFirstClick = true; // Theo dõi lần nhấn đầu tiên của Bot
    private boolean isFirstRound = true; // Theo dõi vòng đầu tiên (người chơi và bot chưa hoàn thành lượt đầu)
    private Random random;
    private Handler handler;
    private String difficulty;
    private int playerFirstMovePosition = -1; // Lưu vị trí ô mà người chơi bấm đầu tiên

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_vs_bot);

        // Lấy mức độ từ Intent
        difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "Easy"; // Mặc định là Easy

        // Khởi tạo bảng lưới 5x5
        mainLayout = findViewById(R.id.main_layout);
        gameGrid = findViewById(R.id.game_grid);
        cellStates = new int[25]; // 5x5 = 25 ô
        initializeBoard();

        // Thiết lập RecyclerView
        cellAdapter = new CellAdapter(cellStates, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5);
        gameGrid.setLayoutManager(gridLayoutManager);
        gameGrid.setAdapter(cellAdapter);
        // Thêm khoảng cách giữa các ô
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        gameGrid.addItemDecoration(new GridSpacingItemDecoration(5, spacingInPixels, true));
        // Đặt background ban đầu cho lượt Người chơi
        updateBackground();

        // Khởi tạo Random và Handler
        random = new Random();
        handler = new Handler(Looper.getMainLooper());
    }

    private void initializeBoard() {
        // Khởi tạo bảng ban đầu: tất cả ô trống
        for (int i = 0; i < cellStates.length; i++) {
            cellStates[i] = 0; // Trống
        }
    }

    private void updateBackground() {
        // Thay đổi background của ConstraintLayout theo lượt
        mainLayout.setBackgroundResource(isPlayer1Turn ? R.drawable.bg_player1 : R.drawable.bg_player2);
        Log.d(TAG, "Updated background, isPlayer1Turn: " + isPlayer1Turn);
    }

    @Override
    public void onCellClick(int position) {
        // Log trạng thái trước khi xử lý
        Log.d(TAG, "onCellClick called, position: " + position + ", isPlayer1Turn: " + isPlayer1Turn);
        printBoardState("Board state before player click:");

        // Chỉ xử lý nếu là lượt của người chơi
        if (!isPlayer1Turn) {
            Log.d(TAG, "Not player's turn, ignoring click at position: " + position);
            return;
        }

        int state = cellStates[position];
        Log.d(TAG, "Player clicked position: " + position + ", state: " + state + ", isPlayer1Turn: " + isPlayer1Turn);

        // Kiểm tra nếu ô trống và là lần nhấn đầu tiên của người chơi
        if (state == 0 && player1FirstClick) {
            cellStates[position] = 3; // Đặt đỏ 3 chấm
            player1FirstClick = false; // Đánh dấu lần nhấn đầu tiên của Người chơi
            playerFirstMovePosition = position; // Lưu vị trí ô đầu tiên
            Log.d(TAG, "Player first click: Placed 3 red dots at position: " + position);
            applyAnimation(position);
            processExplosions(cellStates, position);

            isPlayer1Turn = !isPlayer1Turn; // Chuyển lượt
            updateBackground(); // Cập nhật background
            botTurn(); // Gọi lượt của Bot
            return;
        }

        // Kiểm tra nếu ô thuộc về người chơi (đỏ)
        boolean validClick = false;
        if (state >= 1 && state <= 3) { // Người chơi (đỏ)
            // Thêm kiểm tra nghiêm ngặt để đảm bảo state hợp lệ
            if (state < 1 || state > 3) {
                Log.e(TAG, "ERROR: Invalid state for player's cell at position: " + position + ", state: " + state);
                return;
            }
            if (state < 3) {
                cellStates[position] = state + 1; // Tăng số chấm
                Log.d(TAG, "Increased red dots at position: " + position + ", new state: " + cellStates[position]);
            } else if (state == 3) {
                cellStates[position] = 7; // Tạm thời đặt thành 4 chấm đỏ để kích hoạt nổ
                Log.d(TAG, "Red cell at position: " + position + " reached 4 dots (temp state: 7)");
            }
            validClick = true;
        } else {
            Log.d(TAG, "Invalid click by player at position: " + position + ", state: " + state);
        }

        // Nếu nhấn hợp lệ, áp dụng animation và xử lý nổ
        if (validClick) {
            applyAnimation(position);
            // Log trạng thái ngay sau khi cập nhật để phát hiện lỗi
            Log.d(TAG, "State after update at position: " + position + ", state: " + cellStates[position]);
            printBoardState("Board state after updating cell state:");
            processExplosions(cellStates, position);

            // Kiểm tra điều kiện thắng (chỉ từ lượt thứ hai trở đi)
            if (checkGameOver()) {
                Log.d(TAG, "Game over after player's move, skipping turn switch");
                return;
            }

            isPlayer1Turn = !isPlayer1Turn; // Chuyển lượt
            updateBackground(); // Cập nhật background
            botTurn(); // Gọi lượt của Bot
        }
    }

    private void botTurn() {
        handler.postDelayed(() -> {
            if (isPlayer1Turn) {
                Log.d(TAG, "Not Bot's turn, skipping botTurn");
                return;
            }

            Log.d(TAG, "Bot's turn started, difficulty: " + difficulty);
            printBoardState("Board state before bot move:");

            // Tìm tất cả các ô hợp lệ mà Bot có thể bấm
            ArrayList<Integer> validPositions = new ArrayList<>();
            for (int i = 0; i < cellStates.length; i++) {
                int state = cellStates[i];
                if ((state == 0 && botFirstClick) || (state >= 4 && state <= 6)) {
                    validPositions.add(i);
                }
            }

            // Log danh sách các vị trí hợp lệ để debug
            Log.d(TAG, "Valid positions for Bot: " + validPositions.toString());
            if (validPositions.isEmpty()) {
                Log.d(TAG, "No valid positions for Bot to click, skipping turn");
                isPlayer1Turn = !isPlayer1Turn;
                updateBackground();
                return;
            }

            int position = -1;

            if (difficulty.equals("Easy")) {
                // Mức Easy: Chọn ngẫu nhiên ô hợp lệ
                position = validPositions.get(random.nextInt(validPositions.size()));
            } else if (difficulty.equals("Normal")) {
                // Mức Normal
                if (botFirstClick) {
                    // Bước đầu tiên: Chọn ô trống xa ô của người chơi nhất
                    int maxDistance = -1;
                    int playerRow = playerFirstMovePosition / 5;
                    int playerCol = playerFirstMovePosition % 5;

                    for (int pos : validPositions) {
                        if (cellStates[pos] != 0) continue; // Chỉ xét ô trống
                        int row = pos / 5;
                        int col = pos % 5;
                        int distance = Math.abs(row - playerRow) + Math.abs(col - playerCol); // Khoảng cách Manhattan
                        if (distance > maxDistance) {
                            maxDistance = distance;
                            position = pos;
                        }
                    }
                } else {
                    // Các bước sau: Ưu tiên gây nổ, sau đó chọn ô trống gần ô của người chơi
                    int maxDots = -1;
                    for (int pos : validPositions) {
                        int state = cellStates[pos];
                        if (state >= 4 && state <= 6) { // Ô của bot
                            int dots = state - 3; // Số chấm (1-3)
                            if (dots > maxDots) {
                                maxDots = dots;
                                position = pos;
                            }
                        }
                    }

                    // Nếu không có ô sắp nổ, tìm ô trống gần ô của người chơi
                    if (position == -1) {
                        int maxEnemyNeighbors = -1;
                        for (int pos : validPositions) {
                            if (cellStates[pos] != 0) continue; // Chỉ xét ô trống
                            int row = pos / 5;
                            int col = pos % 5;
                            int enemyNeighbors = 0;

                            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Lên, xuống, trái, phải
                            for (int[] dir : directions) {
                                int newRow = row + dir[0];
                                int newCol = col + dir[1];
                                if (newRow >= 0 && newRow < 5 && newCol >= 0 && newCol < 5) {
                                    int neighborPos = newRow * 5 + newCol;
                                    int state = cellStates[neighborPos];
                                    if (state >= 1 && state <= 3) { // Ô của người chơi
                                        enemyNeighbors++;
                                    }
                                }
                            }

                            if (enemyNeighbors > maxEnemyNeighbors) {
                                maxEnemyNeighbors = enemyNeighbors;
                                position = pos;
                            }
                        }
                    }

                    // Dự phòng: Nếu không tìm thấy ô ưu tiên, chọn ngẫu nhiên
                    if (position == -1) {
                        position = validPositions.get(random.nextInt(validPositions.size()));
                    }
                }
            } else if (difficulty.equals("Hard")) {
                // Mức Hard: Sử dụng Minimax với Alpha-Beta Pruning
                position = findBestMove(validPositions);
            }

            // Kiểm tra lại vị trí được chọn để đảm bảo không phải ô của người chơi
            if (position == -1) {
                Log.e(TAG, "ERROR: Bot failed to select a valid position, selecting random valid position");
                // Chọn một vị trí ngẫu nhiên từ validPositions nếu không tìm được
                position = validPositions.get(random.nextInt(validPositions.size()));
            }

            int state = cellStates[position];
            Log.d(TAG, "Bot (" + difficulty + ") chose position: " + position + ", state before move: " + state);

            // Kiểm tra nếu vị trí được chọn là ô của người chơi (state từ 1 đến 3)
            if (state >= 1 && state <= 3) {
                Log.e(TAG, "ERROR: Bot attempted to click on player's cell (state = " + state + ") at position: " + position);
                // Bỏ qua nước đi này và chọn vị trí khác
                validPositions.remove(Integer.valueOf(position));
                if (validPositions.isEmpty()) {
                    Log.d(TAG, "No valid positions left for Bot after removing invalid move, skipping turn");
                    isPlayer1Turn = !isPlayer1Turn;
                    updateBackground();
                    return;
                }
                // Chọn ngẫu nhiên một vị trí khác từ danh sách còn lại
                position = validPositions.get(random.nextInt(validPositions.size()));
                state = cellStates[position];
                Log.d(TAG, "Bot reselected position: " + position + ", new state: " + state);
            }

            // Xử lý logic bấm của Bot
            if (state == 0 && botFirstClick) {
                cellStates[position] = 6; // Đặt xanh 3 chấm
                botFirstClick = false;
                isFirstRound = false; // Vòng đầu tiên kết thúc sau lượt đầu của bot
                Log.d(TAG, "Bot first click: Placed 3 blue dots at position: " + position);
            } else if (state >= 4 && state <= 6) {
                if (state < 6) {
                    cellStates[position] = state + 1; // Tăng số chấm
                    Log.d(TAG, "Bot increased blue dots at position: " + position + ", new state: " + cellStates[position]);
                } else if (state == 6) {
                    cellStates[position] = 8; // Tạm thời đặt thành 4 chấm xanh để kích hoạt nổ
                    Log.d(TAG, "Blue cell at position: " + position + " reached 4 dots (temp state: 8)");
                }
            } else {
                Log.e(TAG, "ERROR: Bot selected an invalid position after recheck, position: " + position + ", state: " + state);
                isPlayer1Turn = !isPlayer1Turn;
                updateBackground();
                return;
            }

            applyAnimation(position);
            printBoardState("Board state after bot click (before explosion):");
            processExplosions(cellStates, position);

            printBoardState("Board state after bot move:");
            // Kiểm tra điều kiện thắng (chỉ từ lượt thứ hai trở đi)
            if (checkGameOver()) {
                Log.d(TAG, "Game over after bot's move, skipping turn switch");
                return;
            }

            // Đảm bảo chuyển lượt về người chơi
            isPlayer1Turn = !isPlayer1Turn;
            Log.d(TAG, "Turn switched to player, isPlayer1Turn: " + isPlayer1Turn);
            updateBackground();
        }, 1000); // Đợi 1 giây
    }

    private void printBoardState(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");
        for (int i = 0; i < cellStates.length; i++) {
            sb.append(cellStates[i]).append(" ");
            if ((i + 1) % 5 == 0) sb.append("\n");
        }
        Log.d(TAG, sb.toString());
    }

    private boolean checkGameOver() {
        // Không kiểm tra chiến thắng trong vòng đầu tiên
        if (isFirstRound) {
            Log.d(TAG, "Skipping game over check in first round");
            return false;
        }

        int playerCells = 0, botCells = 0;

        // Đếm số ô của người chơi (đỏ) và bot (xanh)
        for (int state : cellStates) {
            if (state >= 1 && state <= 3) playerCells++; // Đỏ (1-3 chấm)
            if (state >= 4 && state <= 6) botCells++; // Xanh (1-3 chấm)
        }

        Log.d(TAG, "Check game over: playerCells = " + playerCells + ", botCells = " + botCells);

        // Kiểm tra điều kiện thắng: Tất cả ô phải cùng một màu
        if (playerCells == 0 && botCells > 0) {
            // Tất cả ô có màu là xanh (không còn ô đỏ)
            Log.d(TAG, "Bot wins: No red cells left, showing dialog");
            showGameOverDialog("Bot thắng!");
            return true;
        } else if (botCells == 0 && playerCells > 0) {
            // Tất cả ô có màu là đỏ (không còn ô xanh)
            Log.d(TAG, "Player wins: No blue cells left, showing dialog");
            showGameOverDialog("Người chơi thắng!");
            return true;
        }

        // Nếu cả hai màu vẫn tồn tại, trò chơi chưa kết thúc
        return false;
    }

    private void showGameOverDialog(String message) {
        Log.d(TAG, "Showing game over dialog with message: " + message);
        new AlertDialog.Builder(this)
                .setTitle("Trò chơi kết thúc")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d(TAG, "Game over dialog OK button clicked, returning to PlayVsBot");
                    // Quay lại màn hình chọn mức độ
                    Intent intent = new Intent(GameVsBotActivity.this, PlayVsBot.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void processExplosions(int[] states, int initialPosition) {
        ArrayList<Integer> cellsToExplode = new ArrayList<>();
        cellsToExplode.add(initialPosition);

        // Keep track of processed positions to avoid infinite loops
        boolean[] processed = new boolean[states.length];
        processed[initialPosition] = true;

        while (!cellsToExplode.isEmpty()) {
            int position = cellsToExplode.remove(0);
            int state = states[position];
            Log.d(TAG, "Processing explosion at position: " + position + ", state: " + state);

            // Only explode if state is 7 (red) or 8 (blue)
            if (state != 7 && state != 8) {
                Log.d(TAG, "No explosion at position: " + position + ", state: " + state + " (requires state 7 or 8 to explode)");
                continue;
            }

            int baseColor = (state == 7) ? 1 : 4; // 1 cho đỏ, 4 cho xanh
            boolean isRedExplosion = (state == 7); // Nổ đỏ hay xanh
            states[position] = 0;
            Log.d(TAG, "Explosion at position: " + position + ", set to empty");
            if (states == cellStates) { // Only apply animation for real game state
                applyAnimation(position);
            }

            // Collect adjacent positions
            int row = position / 5;
            int col = position % 5;
            int[] adjacentPositions = new int[4];
            boolean[] validAdjacent = new boolean[4];

            adjacentPositions[0] = (row - 1) * 5 + col; // Lên
            adjacentPositions[1] = (row + 1) * 5 + col; // Xuống
            adjacentPositions[2] = row * 5 + (col - 1); // Trái
            adjacentPositions[3] = row * 5 + (col + 1); // Phải

            validAdjacent[0] = (row - 1) >= 0;
            validAdjacent[1] = (row + 1) < 5;
            validAdjacent[2] = (col - 1) >= 0;
            validAdjacent[3] = (col + 1) < 5;

            // Update adjacent cells
            for (int i = 0; i < 4; i++) {
                if (validAdjacent[i]) {
                    int adjPos = adjacentPositions[i];
                    if (processed[adjPos]) {
                        Log.d(TAG, "Skipping already processed position: " + adjPos);
                        continue;
                    }
                    processed[adjPos] = true;

                    int adjState = states[adjPos];
                    Log.d(TAG, "Checking adjacent position: " + adjPos + ", state: " + adjState + ", isRedExplosion: " + isRedExplosion);

                    // Tất cả ô lân cận (bao gồm ô của đối thủ) đều bị ảnh hưởng
                    if (adjState == 0) {
                        // Ô trống: Đặt thành màu của bên gây nổ với 1 chấm
                        states[adjPos] = baseColor;
                        Log.d(TAG, "Set adjacent position: " + adjPos + " to base color: " + baseColor);
                    } else if (adjState >= 1 && adjState <= 6) {
                        // Ô có chấm (đỏ hoặc xanh): Tăng số chấm và đổi màu
                        int dots = (adjState <= 3) ? adjState : (adjState - 3); // Số chấm hiện tại
                        dots++; // Tăng 1 chấm
                        if (dots >= 4) {
                            // Nếu đạt 4 chấm, đánh dấu để nổ
                            states[adjPos] = (baseColor == 1) ? 7 : 8;
                            cellsToExplode.add(adjPos);
                            Log.d(TAG, "Adjacent position: " + adjPos + " reached 4 dots, marked to explode, new state: " + states[adjPos]);
                        } else {
                            // Nếu chưa đạt 4 chấm, đổi màu và cập nhật số chấm
                            states[adjPos] = (baseColor == 1) ? dots : (dots + 3);
                            Log.d(TAG, "Adjacent position: " + adjPos + " changed color and increased dots, new state: " + states[adjPos]);
                        }
                    } else {
                        Log.e(TAG, "ERROR: Invalid state at adjacent position: " + adjPos + ", state: " + adjState);
                    }
                    if (states == cellStates) { // Only apply animation for real game state
                        applyAnimation(adjPos);
                    }
                }
            }
        }
    }

    private void applyAnimation(int position) {
        // Cập nhật ô với animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        View view = gameGrid.findViewHolderForAdapterPosition(position).itemView;
        if (view != null) {
            view.startAnimation(fadeIn);
        } else {
            Log.w(TAG, "View is null for position: " + position);
        }
        cellAdapter.updateCell(position, cellStates[position]);
        Log.d(TAG, "Applied animation at position: " + position + ", state: " + cellStates[position]);
    }

    // Hàm đánh giá trạng thái bảng cho Minimax (Hard)
    private int evaluateBoard(int[] states) {
        int score = 0;
        int botCells = 0, playerCells = 0;
        int botExplosiveCells = 0, playerExplosiveCells = 0;
        int botStrategicCells = 0, playerStrategicCells = 0;

        for (int i = 0; i < states.length; i++) {
            int state = states[i];
            int row = i / 5;
            int col = i % 5;

            // Đếm số ô của bot và người chơi
            if (state >= 4 && state <= 6) { // Ô của bot
                botCells++;
                if (state == 6) botExplosiveCells++; // Ô sắp nổ của bot
                // Ưu tiên ô ở góc và cạnh (chiến lược hơn)
                if ((row == 0 || row == 4) && (col == 0 || col == 4)) botStrategicCells += 2; // Góc
                else if (row == 0 || row == 4 || col == 0 || col == 4) botStrategicCells += 1; // Cạnh
            } else if (state >= 1 && state <= 3) { // Ô của người chơi
                playerCells++;
                if (state == 3) playerExplosiveCells++; // Ô sắp nổ của người chơi
                if ((row == 0 || row == 4) && (col == 0 || col == 4)) playerStrategicCells += 2; // Góc
                else if (row == 0 || row == 4 || col == 0 || col == 4) playerStrategicCells += 1; // Cạnh
            }
        }

        // Tính điểm: Ưu tiên số ô, ô sắp nổ, và vị trí chiến lược
        score = (botCells - playerCells) + 2 * (botExplosiveCells - playerExplosiveCells) + (botStrategicCells - playerStrategicCells);
        Log.d(TAG, "Evaluate board: score = " + score + ", botCells = " + botCells + ", playerCells = " + playerCells);
        return score;
    }

    // Kiểm tra điều kiện trò chơi kết thúc trong Minimax
    private int checkGameOverForMinimax(int[] states) {
        if (isFirstRound) {
            return 0; // 0: Trò chơi chưa kết thúc
        }

        int playerCells = 0, botCells = 0;

        for (int state : states) {
            if (state >= 1 && state <= 3) playerCells++; // Đỏ (1-3 chấm)
            if (state >= 4 && state <= 6) botCells++; // Xanh (1-3 chấm)
        }

        if (playerCells == 0 && botCells > 0) {
            return 1000; // Bot thắng
        } else if (botCells == 0 && playerCells > 0) {
            return -1000; // Người chơi thắng
        }
        return 0; // Trò chơi chưa kết thúc
    }

    // Tìm nước đi tốt nhất bằng Minimax với Alpha-Beta Pruning (Hard)
    private int findBestMove(ArrayList<Integer> validPositions) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // Nếu là bước đầu tiên, ưu tiên đặt ở góc hoặc cạnh
        if (botFirstClick) {
            int playerRow = playerFirstMovePosition / 5;
            int playerCol = playerFirstMovePosition % 5;
            int maxDistance = -1;

            // Các vị trí ưu tiên: Góc và cạnh
            int[] priorityPositions = {0, 4, 20, 24, 2, 10, 14, 22}; // Góc: 0, 4, 20, 24; Cạnh: 2, 10, 14, 22
            for (int pos : priorityPositions) {
                if (!validPositions.contains(pos)) continue;
                int row = pos / 5;
                int col = pos % 5;
                int distance = Math.abs(row - playerRow) + Math.abs(col - playerCol);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    bestMove = pos;
                }
            }
            if (bestMove != -1) {
                Log.d(TAG, "Bot first move: Selected priority position: " + bestMove);
                return bestMove;
            }
        }

        // Sử dụng Minimax nếu không phải bước đầu tiên hoặc không tìm thấy vị trí ưu tiên
        for (int pos : validPositions) {
            // Kiểm tra trước khi mô phỏng: Đảm bảo không phải ô của người chơi
            int state = cellStates[pos];
            if (state >= 1 && state <= 3) {
                Log.e(TAG, "ERROR: findBestMove encountered player's cell (state = " + state + ") at position: " + pos);
                continue; // Bỏ qua ô của người chơi
            }

            // Tạo bản sao của trạng thái hiện tại để mô phỏng
            int[] simStates = cellStates.clone();
            boolean simBotFirstClick = botFirstClick;

            // Thực hiện nước đi của bot trên bản sao
            if (simStates[pos] == 0 && simBotFirstClick) {
                simStates[pos] = 6;
                simBotFirstClick = false;
            } else if (simStates[pos] >= 4 && simStates[pos] <= 6) {
                if (simStates[pos] < 6) {
                    simStates[pos]++;
                } else {
                    simStates[pos] = 8;
                }
            } else {
                Log.e(TAG, "ERROR: Invalid state in findBestMove at position: " + pos + ", state: " + simStates[pos]);
                continue;
            }
            processExplosions(simStates, pos); // Áp dụng nổ trên bản sao

            // Gọi Minimax để đánh giá nước đi
            int score = minimax(simStates, simBotFirstClick, 2, false, alpha, beta); // Độ sâu 2
            Log.d(TAG, "Evaluated move at position: " + pos + ", score: " + score);

            // Chỉ cập nhật bestMove nếu nước đi hợp lệ
            if (score > bestScore) {
                bestScore = score;
                bestMove = pos;
                Log.d(TAG, "Updated bestMove to position: " + bestMove + ", score: " + bestScore);
            }

            // Cập nhật alpha
            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) {
                Log.d(TAG, "Alpha-beta pruning at position: " + pos);
                break;
            }
        }

        // Kiểm tra lại bestMove trước khi trả về
        if (bestMove == -1) {
            Log.w(TAG, "No best move found, selecting random valid position");
            bestMove = validPositions.get(random.nextInt(validPositions.size()));
        }

        // Kiểm tra cuối cùng để đảm bảo bestMove không phải ô của người chơi
        int finalState = cellStates[bestMove];
        if (finalState >= 1 && finalState <= 3) {
            Log.e(TAG, "ERROR: findBestMove selected player's cell (state = " + finalState + ") at position: " + bestMove);
            // Chọn một vị trí khác từ validPositions
            bestMove = -1;
            for (int pos : validPositions) {
                int state = cellStates[pos];
                if (state == 0 || (state >= 4 && state <= 6)) {
                    bestMove = pos;
                    break;
                }
            }
            if (bestMove == -1) {
                Log.e(TAG, "ERROR: No valid position found after final check, selecting random position");
                bestMove = validPositions.get(random.nextInt(validPositions.size()));
            }
            Log.d(TAG, "Reassigned bestMove to position: " + bestMove + ", state: " + cellStates[bestMove]);
        }

        Log.d(TAG, "Best move selected: position = " + bestMove + ", score = " + bestScore);
        return bestMove;
    }

    // Thuật toán Minimax với Alpha-Beta Pruning (Hard)
    private int minimax(int[] states, boolean simBotFirstClick, int depth, boolean isMaximizing, int alpha, int beta) {
        // Kiểm tra điều kiện trò chơi kết thúc
        int gameOverScore = checkGameOverForMinimax(states);
        if (gameOverScore != 0) {
            Log.d(TAG, "Minimax: Game over detected, returning score: " + gameOverScore);
            return gameOverScore;
        }

        // Điều kiện dừng: Đạt độ sâu
        if (depth == 0) {
            return evaluateBoard(states);
        }

        ArrayList<Integer> validMoves = new ArrayList<>();
        if (isMaximizing) {
            // Lượt của bot
            for (int i = 0; i < states.length; i++) {
                int state = states[i];
                // Chỉ thêm ô trống (nếu là lần đầu tiên) hoặc ô của bot
                if ((state == 0 && simBotFirstClick) || (state >= 4 && state <= 6)) {
                    validMoves.add(i);
                }
            }
        } else {
            // Lượt của người chơi
            for (int i = 0; i < states.length; i++) {
                int state = states[i];
                if ((state == 0 && player1FirstClick) || (state >= 1 && state <= 3)) {
                    validMoves.add(i);
                }
            }
        }

        // Log danh sách validMoves để debug
        Log.d(TAG, "Minimax depth " + depth + ", isMaximizing: " + isMaximizing + ", validMoves: " + validMoves.toString());
        StringBuilder sb = new StringBuilder();
        sb.append("Board state in minimax (depth ").append(depth).append(", isMaximizing: ").append(isMaximizing).append("):\n");
        for (int i = 0; i < states.length; i++) {
            sb.append(states[i]).append(" ");
            if ((i + 1) % 5 == 0) sb.append("\n");
        }
        Log.d(TAG, sb.toString());

        // Thêm kiểm tra bổ sung để đảm bảo validMoves không chứa ô của người chơi khi isMaximizing = true
        if (isMaximizing) {
            ArrayList<Integer> filteredMoves = new ArrayList<>();
            for (int pos : validMoves) {
                int state = states[pos];
                if (state >= 1 && state <= 3) {
                    Log.e(TAG, "ERROR: validMoves contains player's cell (state = " + state + ") at position: " + pos + " when isMaximizing = true");
                    continue;
                }
                filteredMoves.add(pos);
            }
            validMoves = filteredMoves;
            Log.d(TAG, "Filtered validMoves for bot: " + validMoves.toString());
        }

        if (validMoves.isEmpty()) {
            Log.d(TAG, "Minimax: No valid moves, evaluating board");
            return evaluateBoard(states);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int pos : validMoves) {
                // Kiểm tra lại trước khi mô phỏng
                int state = states[pos];
                if (state >= 1 && state <= 3) {
                    Log.e(TAG, "ERROR: Minimax (maximizing) encountered player's cell (state = " + state + ") at position: " + pos);
                    continue; // Bỏ qua ô của người chơi
                }

                // Tạo bản sao mới để mô phỏng
                int[] simStates = states.clone();
                boolean simBotFirstClickCopy = simBotFirstClick;

                // Thực hiện nước đi của bot trên bản sao
                if (simStates[pos] == 0 && simBotFirstClickCopy) {
                    simStates[pos] = 6;
                    simBotFirstClickCopy = false;
                } else if (simStates[pos] >= 4 && simStates[pos] <= 6) {
                    if (simStates[pos] < 6) {
                        simStates[pos]++;
                    } else {
                        simStates[pos] = 8;
                    }
                } else {
                    Log.e(TAG, "ERROR: Invalid state in minimax (maximizing) at position: " + pos + ", state: " + simStates[pos]);
                    continue;
                }
                processExplosions(simStates, pos);

                // Đệ quy
                int eval = minimax(simStates, simBotFirstClickCopy, depth - 1, false, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                if (beta <= alpha) {
                    Log.d(TAG, "Alpha-beta pruning in maximizing at depth: " + depth);
                    break; // Cắt tỉa
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int pos : validMoves) {
                // Tạo bản sao mới để mô phỏng
                int[] simStates = states.clone();
                boolean simPlayer1FirstClick = player1FirstClick;

                // Thực hiện nước đi của người chơi trên bản sao
                if (simStates[pos] == 0 && simPlayer1FirstClick) {
                    simStates[pos] = 3;
                    simPlayer1FirstClick = false;
                } else if (simStates[pos] >= 1 && simStates[pos] <= 3) {
                    if (simStates[pos] < 3) {
                        simStates[pos]++;
                    } else {
                        simStates[pos] = 7;
                    }
                }
                processExplosions(simStates, pos);

                // Đệ quy
                int eval = minimax(simStates, simBotFirstClick, depth - 1, true, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) {
                    Log.d(TAG, "Alpha-beta pruning in minimizing at depth: " + depth);
                    break; // Cắt tỉa
                }
            }
            return minEval;
        }
    }
}