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
            processExplosions(cellStates, position, () -> {
                if (checkGameOver()) return;
                isPlayer1Turn = false;
                updateBackground();
                botTurn();
            });
            return;
        }

        // Kiểm tra nếu ô thuộc về người chơi (đỏ)
        boolean validClick = false;
        if (state >= 1 && state <= 3) { // Người chơi (đỏ)
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
            Log.d(TAG, "State after update at position: " + position + ", state: " + cellStates[position]);
            printBoardState("Board state after updating cell state:");
            processExplosions(cellStates, position, () -> {
                if (checkGameOver()) return;
                isPlayer1Turn = false;
                updateBackground();
                botTurn();
            });
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

            Log.d(TAG, "Valid positions for Bot: " + validPositions.toString());
            if (validPositions.isEmpty()) {
                Log.d(TAG, "No valid positions for Bot to click, skipping turn");
                isPlayer1Turn = !isPlayer1Turn;
                updateBackground();
                return;
            }

            int position = -1;

            if (difficulty.equals("Easy")) {
                position = validPositions.get(random.nextInt(validPositions.size()));
            } else if (difficulty.equals("Normal")) {
                if (botFirstClick) {
                    int maxDistance = -1;
                    int playerRow = playerFirstMovePosition / 5;
                    int playerCol = playerFirstMovePosition % 5;

                    for (int pos : validPositions) {
                        if (cellStates[pos] != 0) continue;
                        int row = pos / 5;
                        int col = pos % 5;
                        int distance = Math.abs(row - playerRow) + Math.abs(col - playerCol);
                        if (distance > maxDistance) {
                            maxDistance = distance;
                            position = pos;
                        }
                    }
                } else {
                    int maxDots = -1;
                    for (int pos : validPositions) {
                        int state = cellStates[pos];
                        if (state >= 4 && state <= 6) {
                            int dots = state - 3;
                            if (dots > maxDots) {
                                maxDots = dots;
                                position = pos;
                            }
                        }
                    }

                    if (position == -1) {
                        int maxEnemyNeighbors = -1;
                        for (int pos : validPositions) {
                            if (cellStates[pos] != 0) continue;
                            int row = pos / 5;
                            int col = pos % 5;
                            int enemyNeighbors = 0;

                            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                            for (int[] dir : directions) {
                                int newRow = row + dir[0];
                                int newCol = col + dir[1];
                                if (newRow >= 0 && newRow < 5 && newCol >= 0 && newCol < 5) {
                                    int neighborPos = newRow * 5 + newCol;
                                    int state = cellStates[neighborPos];
                                    if (state >= 1 && state <= 3) {
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

                    if (position == -1) {
                        position = validPositions.get(random.nextInt(validPositions.size()));
                    }
                }
            } else if (difficulty.equals("Hard")) {
                position = findBestMove(validPositions);
            }

            if (position == -1) {
                Log.e(TAG, "ERROR: Bot failed to select a valid position, selecting random valid position");
                position = validPositions.get(random.nextInt(validPositions.size()));
            }

            int state = cellStates[position];
            Log.d(TAG, "Bot (" + difficulty + ") chose position: " + position + ", state before move: " + state);

            if (state >= 1 && state <= 3) {
                Log.e(TAG, "ERROR: Bot attempted to click on player's cell (state = " + state + ") at position: " + position);
                validPositions.remove(Integer.valueOf(position));
                if (validPositions.isEmpty()) {
                    Log.d(TAG, "No valid positions left for Bot after removing invalid move, skipping turn");
                    isPlayer1Turn = !isPlayer1Turn;
                    updateBackground();
                    return;
                }
                position = validPositions.get(random.nextInt(validPositions.size()));
                state = cellStates[position];
                Log.d(TAG, "Bot reselected position: " + position + ", new state: " + state);
            }

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

            processExplosions(cellStates, position, () -> {
                if (checkGameOver()) return;
                isPlayer1Turn = true;
                updateBackground();
            });

        }, 1250); // Đợi 1 giây
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
        if (isFirstRound) {
            Log.d(TAG, "Skipping game over check in first round");
            return false;
        }

        int playerCells = 0, botCells = 0;

        for (int state : cellStates) {
            if (state >= 1 && state <= 3) playerCells++; // Đỏ (1-3 chấm)
            if (state >= 4 && state <= 6) botCells++; // Xanh (1-3 chấm)
        }

        Log.d(TAG, "Check game over: playerCells = " + playerCells + ", botCells = " + botCells);

        if (playerCells == 0 && botCells > 0) {
            Log.d(TAG, "Bot wins: No red cells left, showing dialog");
            showGameOverDialog("Bot thắng!");
            return true;
        } else if (botCells == 0 && playerCells > 0) {
            Log.d(TAG, "Player wins: No blue cells left, showing dialog");
            showGameOverDialog("Người chơi thắng!");
            return true;
        }

        return false;
    }

    private void showGameOverDialog(String message) {
        Log.d(TAG, "Showing game over dialog with message: " + message);
        new AlertDialog.Builder(this)
                .setTitle("Trò chơi kết thúc")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d(TAG, "Game over dialog OK button clicked, returning to PlayVsBot");
                    Intent intent = new Intent(GameVsBotActivity.this, PlayVsBot.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void processExplosions(int[] states, int initialPosition, Runnable onComplete) {
        ArrayList<Integer> cellsToExplode = new ArrayList<>();
        cellsToExplode.add(initialPosition);

        boolean[] processed = new boolean[states.length];
        processed[initialPosition] = true;

        processNextExplosion(states, cellsToExplode, processed, onComplete);
    }

    private void processNextExplosion(int[] states, ArrayList<Integer> cellsToExplode, boolean[] processed, Runnable onComplete) {
        if (cellsToExplode.isEmpty()) {
            if (states == cellStates && onComplete != null) {
                onComplete.run();
            }
            return;
        }

        int position = cellsToExplode.remove(0);
        int state = states[position];

        if (state != 7 && state != 8) {
            processNextExplosion(states, cellsToExplode, processed, onComplete);
            return;
        }

        final int posToExplode = position;
        final int colorBase = (state == 7) ? 1 : 4;
        final boolean isRed = (state == 7);

        if (states == cellStates) {
            cellAdapter.updateCell(posToExplode, state);
            final View view = gameGrid.findViewHolderForAdapterPosition(posToExplode).itemView;
            if (view != null) {
                Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                view.startAnimation(fadeIn);
            }
        }

        handler.postDelayed(() -> {
            states[posToExplode] = 0;
            if (states == cellStates) {
                applyAnimation(posToExplode);
            }

            int row = posToExplode / 5;
            int col = posToExplode % 5;
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            ArrayList<Integer> newExplosions = new ArrayList<>();

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < 5 && newCol >= 0 && newCol < 5) {
                    int adjPos = newRow * 5 + newCol;

                    if (processed[adjPos]) continue;
                    processed[adjPos] = true;

                    int adjState = states[adjPos];
                    int dots = (adjState <= 3) ? adjState : (adjState >= 4 && adjState <= 6 ? adjState - 3 : 0);
                    dots++;

                    if (dots >= 4) {
                        states[adjPos] = isRed ? 7 : 8;
                        newExplosions.add(adjPos);
                    } else {
                        states[adjPos] = isRed ? dots : (dots + 3);
                    }

                    if (states == cellStates) {
                        applyAnimation(adjPos);
                    }
                }
            }

            if (!newExplosions.isEmpty()) {
                handler.postDelayed(() -> {
                    for (int i = newExplosions.size() - 1; i >= 0; i--) {
                        cellsToExplode.add(0, newExplosions.get(i));
                    }
                    processNextExplosion(states, cellsToExplode, processed, onComplete);
                }, 200);
            } else {
                processNextExplosion(states, cellsToExplode, processed, onComplete);
            }
        }, 500);
    }

    private void applyAnimation(int position) {
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

    // Hàm đánh giá trạng thái bàn cờ
    private int evaluateBoard(int[] states) {
        int score = 0;
        int botCells = 0, playerCells = 0;
        int botExplosiveCells = 0, playerExplosiveCells = 0;
        int botStrategicCells = 0, playerStrategicCells = 0;
        int botPotentialExplosions = 0, playerPotentialExplosions = 0;

        for (int i = 0; i < states.length; i++) {
            int state = states[i];
            int row = i / 5;
            int col = i % 5;

            if (state >= 4 && state <= 6) { // Ô của bot (xanh)
                botCells++;
                if (state == 6) botExplosiveCells++;
                botPotentialExplosions += countEnemyNeighbors(states, i, true);
                if ((row == 0 || row == 4) && (col == 0 || col == 4)) botStrategicCells += 3; // Góc
                else if (row == 0 || row == 4 || col == 0 || col == 4) botStrategicCells += 1; // Cạnh
            } else if (state >= 1 && state <= 3) { // Ô của người chơi (đỏ)
                playerCells++;
                if (state == 3) playerExplosiveCells++;
                playerPotentialExplosions += countEnemyNeighbors(states, i, false);
                if ((row == 0 || row == 4) && (col == 0 || col == 4)) playerStrategicCells += 3; // Góc
                else if (row == 0 || row == 4 || col == 0 || col == 4) playerStrategicCells += 1; // Cạnh
            }
        }

        score = (botCells - playerCells) * 10 +
                (botExplosiveCells - playerExplosiveCells) * 20 +
                (botStrategicCells - playerStrategicCells) * 5 +
                (botPotentialExplosions - playerPotentialExplosions) * 15;

        if (playerCells == 0 && botCells > 0) score += 1000;
        if (botCells == 0 && playerCells > 0) score -= 1000;

        Log.d(TAG, "Evaluate board: score = " + score + ", botCells = " + botCells + ", playerCells = " + playerCells);
        return score;
    }

    // Đếm số ô của đối thủ xung quanh ô tại vị trí position
    private int countEnemyNeighbors(int[] states, int position, boolean isBot) {
        int row = position / 5;
        int col = position % 5;
        int count = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (newRow >= 0 && newRow < 5 && newCol >= 0 && newCol < 5) {
                int neighborPos = newRow * 5 + newCol;
                int state = states[neighborPos];
                if (isBot && state >= 1 && state <= 3) count++;
                if (!isBot && state >= 4 && state <= 6) count++;
            }
        }
        return count;
    }

    // Kiểm tra điều kiện trò chơi kết thúc trong Minimax
    private int checkGameOverForMinimax(int[] states) {
        if (isFirstRound) {
            return 0;
        }

        int playerCells = 0, botCells = 0;

        for (int state : states) {
            if (state >= 1 && state <= 3) playerCells++;
            if (state >= 4 && state <= 6) botCells++;
        }

        if (playerCells == 0 && botCells > 0) {
            return 1000; // Bot thắng
        } else if (botCells == 0 && playerCells > 0) {
            return -1000; // Người chơi thắng
        }
        return 0;
    }

    // Tìm nước đi tốt nhất bằng Minimax với Alpha-Beta Pruning
    private int findBestMove(ArrayList<Integer> validPositions) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        if (botFirstClick) {
            // Các vị trí tương đối so với ô của người chơi: -4, -6, 4, 6
            int[] offsets = {-6, -4, 4, 6};
            ArrayList<Integer> priorityPositions = new ArrayList<>();

            int playerRow = playerFirstMovePosition / 5;
            int playerCol = playerFirstMovePosition % 5;

            // Kiểm tra từng offset
            for (int offset : offsets) {
                int pos = playerFirstMovePosition + offset;
                // Kiểm tra tính hợp lệ của vị trí
                if (pos >= 0 && pos < 25 && cellStates[pos] == 0 && validPositions.contains(pos)) {
                    int row = pos / 5;
                    int col = pos % 5;
                    // Đảm bảo không cùng hàng hoặc cột với ô của người chơi
                    if (row != playerRow && col != playerCol) {
                        priorityPositions.add(pos);
                    }
                }
            }

            // Nếu có vị trí hợp lệ, chọn ngẫu nhiên từ danh sách priorityPositions
            if (!priorityPositions.isEmpty()) {
                bestMove = priorityPositions.get(random.nextInt(priorityPositions.size()));
                Log.d(TAG, "Bot first move: Selected position " + bestMove + " from priority positions " + priorityPositions);
                return bestMove;
            } else {
                // Nếu không có vị trí nào hợp lệ, chọn ngẫu nhiên từ validPositions
                for (int pos : validPositions) {
                    if (cellStates[pos] == 0) {
                        bestMove = pos;
                        break;
                    }
                }
                if (bestMove != -1) {
                    Log.d(TAG, "Bot first move: No priority positions available, selected random empty position " + bestMove);
                    return bestMove;
                }
            }
        }

        // Sử dụng Minimax cho các nước đi tiếp theo
        for (int pos : validPositions) {
            int state = cellStates[pos];
            if (state >= 1 && state <= 3) {
                Log.e(TAG, "ERROR: findBestMove encountered player's cell (state = " + state + ") at position: " + pos);
                continue;
            }

            int[] simStates = cellStates.clone();
            boolean simBotFirstClick = botFirstClick;

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

            processExplosionsForMinimax(simStates, pos, null);

            int score = minimax(simStates, simBotFirstClick, 3, false, alpha, beta);
            Log.d(TAG, "Evaluated move at position: " + pos + ", score: " + score);

            if (score > bestScore) {
                bestScore = score;
                bestMove = pos;
                Log.d(TAG, "Updated bestMove to position: " + bestMove + ", score: " + bestScore);
            }

            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) {
                Log.d(TAG, "Alpha-beta pruning at position: " + pos);
                break;
            }
        }

        if (bestMove == -1) {
            Log.w(TAG, "No best move found, selecting random valid position");
            bestMove = validPositions.get(random.nextInt(validPositions.size()));
        }

        Log.d(TAG, "Best move selected: position = " + bestMove + ", score = " + bestScore);
        return bestMove;
    }

    // Thuật toán Minimax với Alpha-Beta Pruning
    private int minimax(int[] states, boolean simBotFirstClick, int depth, boolean isMaximizing, int alpha, int beta) {
        int gameOverScore = checkGameOverForMinimax(states);
        if (gameOverScore != 0) {
            Log.d(TAG, "Minimax: Game over detected, returning score: " + gameOverScore);
            return gameOverScore;
        }

        if (depth == 0) {
            return evaluateBoard(states);
        }

        ArrayList<Integer> validMoves = new ArrayList<>();
        if (isMaximizing) {
            for (int i = 0; i < states.length; i++) {
                int state = states[i];
                if ((state == 0 && simBotFirstClick) || (state >= 4 && state <= 6)) {
                    validMoves.add(i);
                }
            }
        } else {
            for (int i = 0; i < states.length; i++) {
                int state = states[i];
                if ((state == 0 && player1FirstClick) || (state >= 1 && state <= 3)) {
                    validMoves.add(i);
                }
            }
        }

        if (validMoves.isEmpty()) {
            Log.d(TAG, "Minimax: No valid moves, evaluating board");
            return evaluateBoard(states);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int pos : validMoves) {
                int state = states[pos];
                if (state >= 1 && state <= 3) {
                    Log.e(TAG, "ERROR: Minimax (maximizing) encountered player's cell (state = " + state + ") at position: " + pos);
                    continue;
                }

                int[] simStates = states.clone();
                boolean simBotFirstClickCopy = simBotFirstClick;

                if (simStates[pos] == 0 && simBotFirstClickCopy) {
                    simStates[pos] = 6;
                    simBotFirstClickCopy = false;
                } else if (simStates[pos] >= 4 && simStates[pos] <= 6) {
                    if (simStates[pos] < 6) {
                        simStates[pos]++;
                    } else {
                        simStates[pos] = 8;
                    }
                }

                processExplosionsForMinimax(simStates, pos, null);

                int eval = minimax(simStates, simBotFirstClickCopy, depth - 1, false, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                if (beta <= alpha) {
                    Log.d(TAG, "Alpha-beta pruning in maximizing at depth: " + depth);
                    break;
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int pos : validMoves) {
                int[] simStates = states.clone();
                boolean simPlayer1FirstClick = player1FirstClick;

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

                processExplosionsForMinimax(simStates, pos, null);

                int eval = minimax(simStates, simBotFirstClick, depth - 1, true, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) {
                    Log.d(TAG, "Alpha-beta pruning in minimizing at depth: " + depth);
                    break;
                }
            }
            return minEval;
        }
    }

    // Xử lý nổ cho mô phỏng trong Minimax (không ảnh hưởng đến UI)
    private void processExplosionsForMinimax(int[] states, int initialPosition, Runnable onComplete) {
        ArrayList<Integer> cellsToExplode = new ArrayList<>();
        cellsToExplode.add(initialPosition);

        boolean[] processed = new boolean[states.length];
        processed[initialPosition] = true;

        while (!cellsToExplode.isEmpty()) {
            int position = cellsToExplode.remove(0);
            int state = states[position];

            if (state != 7 && state != 8) continue;

            states[position] = 0;
            int row = position / 5;
            int col = position % 5;
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            boolean isRed = (state == 7);

            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < 5 && newCol >= 0 && newCol < 5) {
                    int adjPos = newRow * 5 + newCol;
                    if (processed[adjPos]) continue;
                    processed[adjPos] = true;

                    int adjState = states[adjPos];
                    int dots = (adjState <= 3) ? adjState : (adjState >= 4 && adjState <= 6 ? adjState - 3 : 0);
                    dots++;

                    if (dots >= 4) {
                        states[adjPos] = isRed ? 7 : 8;
                        cellsToExplode.add(adjPos);
                    } else {
                        states[adjPos] = isRed ? dots : (dots + 3);
                    }
                }
            }
        }

        if (onComplete != null) {
            onComplete.run();
        }
    }
}