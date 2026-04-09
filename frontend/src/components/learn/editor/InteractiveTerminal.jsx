'use client';

import { useEffect, useRef, useCallback } from 'react';
import { Terminal as XTerm } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

const WS_BASE = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/^http/, 'ws') ?? 'ws://localhost:8081';

export default function InteractiveTerminal({ code, language, running, onExit }) {
  const containerRef = useRef(null);
  const termRef = useRef(null);
  const wsRef = useRef(null);
  const fitRef = useRef(null);
  const inputBufferRef = useRef('');

  const cleanup = useCallback(() => {
    if (wsRef.current) {
      if (wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: 'kill' }));
      }
      wsRef.current.close();
      wsRef.current = null;
    }
  }, []);

  // Initialize xterm once
  useEffect(() => {
    if (!containerRef.current || termRef.current) return;

    const term = new XTerm({
      theme: {
        background: '#0e0e1a',
        foreground: '#c0c0d8',
        cursor: '#c0c0d8',
        selectionBackground: '#3a3a5e',
        black: '#0e0e1a',
        red: '#f87171',
        green: '#34d399',
        yellow: '#fbbf24',
        blue: '#60a5fa',
        magenta: '#c084fc',
        cyan: '#22d3ee',
        white: '#c0c0d8',
      },
      fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', Menlo, monospace",
      fontSize: 13,
      lineHeight: 1.4,
      cursorBlink: true,
      convertEol: true,
    });

    const fit = new FitAddon();
    term.loadAddon(fit);
    term.open(containerRef.current);
    fit.fit();

    termRef.current = term;
    fitRef.current = fit;

    term.writeln('\x1b[90m$ Press Run to execute your code\x1b[0m');

    return () => {
      cleanup();
      term.dispose();
      termRef.current = null;
      fitRef.current = null;
    };
  }, [cleanup]);

  // Resize handling
  useEffect(() => {
    const handleResize = () => fitRef.current?.fit();
    const observer = new ResizeObserver(handleResize);
    if (containerRef.current) observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  // Start/stop based on `running` prop
  useEffect(() => {
    if (!running || !termRef.current) return;

    const term = termRef.current;
    term.reset();
    term.writeln('\x1b[90m$ Running... (type here if program expects input)\x1b[0m\r\n');
    inputBufferRef.current = '';

    // Connect WebSocket
    const ws = new WebSocket(`${WS_BASE}/ws/terminal`);
    wsRef.current = ws;

    ws.onopen = () => {
      ws.send(JSON.stringify({ type: 'start', code, language }));
      // Auto-focus terminal so user can type immediately
      setTimeout(() => term.focus(), 100);
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg.type === 'stdout') {
          term.write(msg.data);
        } else if (msg.type === 'stderr') {
          term.write(`\x1b[31m${msg.data}\x1b[0m`);
        } else if (msg.type === 'exit') {
          const exitCode = parseInt(msg.data, 10);
          if (!isNaN(exitCode)) {
            term.writeln(`\r\n\x1b[90m\r\nProcess exited with code ${exitCode}\x1b[0m`);
          } else {
            term.writeln(`\r\n\x1b[90m\r\n${msg.data}\x1b[0m`);
          }
          onExit?.();
        } else if (msg.type === 'error') {
          term.writeln(`\r\n\x1b[31mError: ${msg.data}\x1b[0m`);
          onExit?.();
        }
      } catch {
        // non-JSON message
      }
    };

    ws.onerror = () => {
      term.writeln('\r\n\x1b[31mWebSocket connection failed\x1b[0m');
      onExit?.();
    };

    ws.onclose = () => {
      // noop — exit message already handled
    };

    // Handle keyboard input
    const onData = term.onData((data) => {
      if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return;

      // Ctrl+C → kill
      if (data === '\x03') {
        term.writeln('^C');
        wsRef.current.send(JSON.stringify({ type: 'kill' }));
        return;
      }

      // Enter
      if (data === '\r') {
        term.write('\r\n');
        wsRef.current.send(JSON.stringify({ type: 'stdin', data: inputBufferRef.current + '\n' }));
        inputBufferRef.current = '';
        return;
      }

      // Backspace
      if (data === '\x7f' || data === '\b') {
        if (inputBufferRef.current.length > 0) {
          inputBufferRef.current = inputBufferRef.current.slice(0, -1);
          term.write('\b \b');
        }
        return;
      }

      // Regular character — local echo + buffer
      inputBufferRef.current += data;
      term.write(data);
    });

    return () => {
      onData.dispose();
      cleanup();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [running]);

  return (
    <div
      ref={containerRef}
      className='w-full h-full min-h-[120px] cursor-text'
      style={{ padding: '8px 4px' }}
      onClick={() => termRef.current?.focus()}
    />
  );
}
