"use client";

import { useEffect, useRef } from "react";

interface Particle {
    x: number;
    y: number;
    vx: number;
    vy: number;
    baseVx: number;
    baseVy: number;
    size: number;
    baseAlpha: number;
}

interface Ripple {
    x: number;
    y: number;
    radius: number;
    maxRadius: number;
    alpha: number;
}

export default function InteractiveParticleBackground() {
    const canvasRef = useRef<HTMLCanvasElement | null>(null);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const ctx = canvas.getContext("2d");
        if (!ctx) return;

        let animationFrameId: number;
        let width = (canvas.width = window.innerWidth);
        let height = (canvas.height = window.innerHeight);

        const mouse = {
            x: -1000,
            y: -1000,
            radius: 180,
        };

        const ripples: Ripple[] = [];

        const handleResize = () => {
            if (!canvas) return;
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
            initParticles();
        };

        const handleMouseMove = (e: MouseEvent) => {
            mouse.x = e.clientX;
            mouse.y = e.clientY;
        };

        const handleMouseLeave = () => {
            mouse.x = -1000;
            mouse.y = -1000;
        };

        // Click chuột tạo hiệu ứng nổ bung các hạt ra xung quanh
        const handleMouseDown = (e: MouseEvent) => {
            const clickX = e.clientX;
            const clickY = e.clientY;

            // 1. Tạo vòng sóng xung kích (Shockwave ripple)
            ripples.push({
                x: clickX,
                y: clickY,
                radius: 8,
                maxRadius: 130,
                alpha: 0.65,
            });

            // 2. Đẩy văng tất cả các hạt ở gần ra xa với vận tốc lớn
            for (let i = 0; i < particles.length; i++) {
                const p = particles[i];
                const dx = p.x - clickX;
                const dy = p.y - clickY;
                const dist = Math.sqrt(dx * dx + dy * dy) || 1;

                if (dist < 280) {
                    const angle = Math.atan2(dy, dx);
                    const power = Math.max(10, ((280 - dist) / 280) * 26);
                    p.vx = Math.cos(angle) * power;
                    p.vy = Math.sin(angle) * power;
                }
            }
        };

        window.addEventListener("resize", handleResize);
        window.addEventListener("mousemove", handleMouseMove);
        window.addEventListener("mouseleave", handleMouseLeave);
        window.addEventListener("mousedown", handleMouseDown);

        // Khởi tạo số lượng hạt vừa vặn
        const particleCount = Math.min(Math.floor((width * height) / 13000), 80);
        let particles: Particle[] = [];

        const initParticles = () => {
            particles = [];
            for (let i = 0; i < particleCount; i++) {
                const bvx = (Math.random() - 0.5) * 0.8;
                const bvy = (Math.random() - 0.5) * 0.8;
                particles.push({
                    x: Math.random() * width,
                    y: Math.random() * height,
                    vx: bvx,
                    vy: bvy,
                    baseVx: bvx,
                    baseVy: bvy,
                    size: Math.random() * 2 + 1.8,
                    baseAlpha: Math.random() * 0.4 + 0.35,
                });
            }
        };

        initParticles();

        const render = () => {
            ctx.clearRect(0, 0, width, height);

            // 1. Vẽ và cập nhật các vòng sóng nổ (Shockwave ripples)
            for (let rIdx = ripples.length - 1; rIdx >= 0; rIdx--) {
                const rip = ripples[rIdx];
                rip.radius += 4.5;
                rip.alpha *= 0.94;

                ctx.beginPath();
                ctx.arc(rip.x, rip.y, rip.radius, 0, Math.PI * 2);
                ctx.strokeStyle = `rgba(99, 102, 241, ${rip.alpha})`;
                ctx.lineWidth = 2.5;
                ctx.stroke();

                if (rip.alpha < 0.02 || rip.radius > rip.maxRadius) {
                    ripples.splice(rIdx, 1);
                }
            }

            // 2. Cập nhật và vẽ các hạt
            for (let i = 0; i < particles.length; i++) {
                const p = particles[i];

                const dx = mouse.x - p.x;
                const dy = mouse.y - p.y;
                const dist = Math.sqrt(dx * dx + dy * dy) || 1;

                // Xử lý lực tương tác với chuột
                if (dist < mouse.radius && mouse.x > 0) {
                    const minOrbitDist = 45; // Giữ khoảng cách tối thiểu để không bị dính thành 1 cục

                    if (dist > minOrbitDist) {
                        // Hút nhẹ nhàng về phía chuột
                        const force = (mouse.radius - dist) / mouse.radius;
                        p.vx += (dx / dist) * force * 0.5;
                        p.vy += (dy / dist) * force * 0.5;
                    } else {
                        // Khi vào quá gần (< 45px) thì đẩy nhẹ ra để duy trì hình đám mây hạt quanh chuột
                        p.vx -= (dx / dist) * 0.6;
                        p.vy -= (dy / dist) * 0.6;
                    }

                    // Ma sát khi ở gần chuột để chuyển động êm
                    p.vx *= 0.93;
                    p.vy *= 0.93;
                } else {
                    // Trôi tự nhiên và dần hồi phục lại vận tốc ban đầu
                    p.vx = p.vx * 0.96 + p.baseVx * 0.04;
                    p.vy = p.vy * 0.96 + p.baseVy * 0.04;
                }

                // Cập nhật tọa độ
                p.x += p.vx;
                p.y += p.vy;

                // Xử lý chạm viền màn hình thì dội lại
                if (p.x < 0) {
                    p.x = 0;
                    p.vx *= -1;
                    p.baseVx *= -1;
                } else if (p.x > width) {
                    p.x = width;
                    p.vx *= -1;
                    p.baseVx *= -1;
                }

                if (p.y < 0) {
                    p.y = 0;
                    p.vy *= -1;
                    p.baseVy *= -1;
                } else if (p.y > height) {
                    p.y = height;
                    p.vy *= -1;
                    p.baseVy *= -1;
                }

                // 3. Vẽ hạt
                ctx.beginPath();
                ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
                ctx.fillStyle = dist < mouse.radius
                    ? `rgba(79, 70, 229, ${Math.min(0.9, p.baseAlpha + 0.35)})`
                    : `rgba(99, 102, 241, ${p.baseAlpha})`;
                ctx.fill();

                // 4. Nối các đường line mảnh giữa các hạt ở gần nhau
                for (let j = i + 1; j < particles.length; j++) {
                    const p2 = particles[j];
                    const lineDx = p.x - p2.x;
                    const lineDy = p.y - p2.y;
                    const lineDist = Math.sqrt(lineDx * lineDx + lineDy * lineDy);

                    if (lineDist < 110) {
                        const alpha = (1 - lineDist / 110) * 0.28;
                        ctx.beginPath();
                        ctx.moveTo(p.x, p.y);
                        ctx.lineTo(p2.x, p2.y);
                        ctx.strokeStyle = `rgba(99, 102, 241, ${alpha})`;
                        ctx.lineWidth = 0.8;
                        ctx.stroke();
                    }
                }

                // 5. Nối đường line mảnh giữa hạt và con trỏ chuột nếu ở trong tầm
                if (dist < mouse.radius && mouse.x > 0) {
                    const mouseLineAlpha = (1 - dist / mouse.radius) * 0.35;
                    ctx.beginPath();
                    ctx.moveTo(p.x, p.y);
                    ctx.lineTo(mouse.x, mouse.y);
                    ctx.strokeStyle = `rgba(79, 70, 229, ${mouseLineAlpha})`;
                    ctx.lineWidth = 0.9;
                    ctx.stroke();
                }
            }

            animationFrameId = requestAnimationFrame(render);
        };

        render();

        return () => {
            window.removeEventListener("resize", handleResize);
            window.removeEventListener("mousemove", handleMouseMove);
            window.removeEventListener("mouseleave", handleMouseLeave);
            window.removeEventListener("mousedown", handleMouseDown);
            cancelAnimationFrame(animationFrameId);
        };
    }, []);

    return (
        <canvas
            ref={canvasRef}
            className="pointer-events-none fixed inset-0 z-0"
            aria-hidden="true"
        />
    );
}

